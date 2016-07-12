package clickbaitModel;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import experiment.feature.Feature;
import experiment.feature.FeatureLists;
import experiment.feature.text.MessageTextFeature;
import message.Message;
import message.MessageFactory;
import message.TestMessage;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterObjectFactory;
import twitter4j.conf.ConfigurationBuilder;
import util.TwitterUtil;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
//import weka.core.Instances;
import weka.core.Instances;

/**
 * Servlet implementation class ClickbaitModel
 */
@WebServlet("/ClickbaitModel")
public class ClickbaitModel extends HttpServlet {

	private static final long serialVersionUID = 1L;
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	Instances referenceDataSet;
	Classifier cls;
	Map<String, Model> nameToModel = new HashMap<String, Model>();
	Properties props;
	Twitter twitter;
	String defaultModelName = "message";

	public ClickbaitModel() throws Exception {
		super();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			initModels();
		} catch (Exception e) {
			throw new ServletException(e);
		}
		initTwitter();
	}

	public void initModels() throws Exception {
		if (nameToModel.isEmpty()) {
			try {
				loadTextOnlyModel();
			} catch (Exception e1) {
				System.out.println("Error on loading text model");
				e1.printStackTrace();
			}
			try {
				loadMessageModel();
			} catch (Exception e1) {
				System.out.println("Error on loading message model");
				e1.printStackTrace();
			}
		}
	}

	private void loadTextOnlyModel() throws Exception, IOException {
		System.out.println("Started loading text model");
		List<Feature> text = new LinkedList<Feature>();
		Feature textFeature = new MessageTextFeature();
		textFeature.setCategory("Msg");
		text.add(textFeature);
		Model defaultModel = null;
		defaultModel = new Model(readFile("/WEB-INF/message-text.arff"), text);
		defaultModel.setName("text-only");
		Classifier classifier = new RandomForest();
		defaultModel.trainClassifier(classifier);
		nameToModel.put("text-only", defaultModel);
		System.out.println("Finished loading text model");
	}

	private void loadMessageModel() throws Exception, IOException {
		System.out.println("Started loading message model");
		List<Feature> message = FeatureLists.getMessageFeatures();
		Model messageModel = new Model(readFile("/WEB-INF/message-text.arff"), message);
		messageModel.setName("message");
		Classifier classifier = new RandomForest();
		messageModel.trainClassifier(classifier);
		nameToModel.put("message", messageModel);
		System.out.println("Finished loading message model");
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		if (nameToModel.size() < 1) {
			try {
				initModels();
				System.out.println("loaded models");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		res.setContentType("application/json");
		PrintWriter pw = res.getWriter();
		String response = "";
		try {
			if (req != null) {
				response = serveGet(req).toString();
			} else {
				response = "Request not understood";
			}

		} catch (TwitterException twitter) {
			response = "Twitter Error Occured, Tweet not found or request limit reached";
		} catch (Exception e) {
			e.printStackTrace();
			response = getErrorMessage();
		}

		pw.println(response);

	}

	private String getErrorMessage() {
		String response;
		response = "ERROR";
		return response;
	}

	private void initTwitter() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey("A1e6lZD829fbGBPZbAbqBRJLd")
				.setOAuthConsumerSecret("jsvCt3tOp9hdOf0xBZYmccRmjQne7Fh3MDvQf3CXz3IPmckIx5")
				.setOAuthAccessToken("3280097199-I9Q4dEtsX2qr9BnT5n9mTPeYlYV3Fm1yBMV5231")
				.setOAuthAccessTokenSecret("jk08WJwHeYyK7WWK1ljhuswIc8LFuivMvwB09vKedkPN4")
				.setHttpConnectionTimeout(100000).setJSONStoreEnabled(true);
		TwitterFactory tf = new TwitterFactory(cb.build());
		twitter = tf.getInstance();
	}

	private List<Response> serveGet(HttpServletRequest req) throws Exception {
		List<Response> responses = new LinkedList<>();
		if (req.getParameter("ID") != null) {
			responses.add(handleID(req));
		} else {
			if (req.getParameter("text") != null) {
				responses.add(handleText(req));
			} else {
				if (req.getParameter("user") != null) {
					responses.addAll(handleUser(req));
				}
			}
		}
		return responses;
	}

	private Response handleID(HttpServletRequest req) throws Exception {
		Model model = getModel(req);
		String ID = req.getParameter("ID");
		Status status = TwitterUtil.updateStatus(Long.valueOf(ID));
		Message message = MessageFactory.getMessage(status);
		Response response = model.getpredictedClass(message);

		return response;
	}

	private Response handleText(HttpServletRequest req) throws Exception {
		Model model = getModel(req);
		TestMessage message = new TestMessage("-1", req.getParameter("text"));
		Response response = model.getpredictedClass(message);
		return response;
	}

	private List<Response> handleUser(HttpServletRequest req) throws Exception {
		List<Response> responses = new LinkedList<Response>();
		List<Status> stati = getUserTimeLine(req.getParameter("user"));
		Model model = getModel(req);
		for (Status status : stati) {
			Message message = MessageFactory.getMessage(status);
			try {
				Response response = model.getpredictedClass(message);
				responses.add(response);
				System.out.println(response.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return responses;
	}

	public List<Status> getUserTimeLine(String user) {
		List<Status> statuses = new LinkedList<Status>();
		try {
			statuses = twitter.getUserTimeline(user);
		} catch (TwitterException twitterException) {
			twitterException.printStackTrace();
			int secToReset = twitterException.getRateLimitStatus().getSecondsUntilReset();
			try {
				Thread.sleep(1100 * secToReset);
				statuses = getUserTimeLine(user);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
		return statuses;
	}

	private Model getModel(HttpServletRequest req) {
		Model model = nameToModel.get(defaultModelName);
		String requestetModelName = req.getParameter("model");
		if (requestetModelName != null) {
			if (nameToModel.containsKey(requestetModelName)) {
				model = nameToModel.get(requestetModelName);
			}
		} else {
			if (req.getParameter("text") != null) {
				if (nameToModel.containsKey("text-only"))
					model = nameToModel.get("text-only");
			}
		}
		return model;
	}

	private String readFile(String fName) throws IOException {
		ServletContext cntxt = getServletContext();
		InputStream ins = cntxt.getResourceAsStream(fName);
		String fContent = "";
		if (ins != null) {
			InputStreamReader isr = new InputStreamReader(ins);
			BufferedReader reader = new BufferedReader(isr);
			String word = "";
			while ((word = reader.readLine()) != null) {
				fContent += (word) + "\n";
			}
		}
		return fContent;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		System.out.println(request);
		System.out.println(request.getParameterMap());
		if (request.getParameter("ID") != null) {
			String id = request.getParameter("ID");
			Status status = TwitterUtil.updateStatus(Long.valueOf(id));
			Message message = MessageFactory.getMessage(status);
			if (request.getParameter("annotation") != null) {
				saveAnnotation(message, request);
			}
		}

		doGet(request, response);
	}

	private void saveAnnotation(Message message, HttpServletRequest request) throws IOException {
		File requestString = new File("request.json");
		try (InputStream input = request.getInputStream()) {
			Files.copy(input, requestString.toPath());
		}
		File status = new File("/status.json");
		String rawJSON = TwitterObjectFactory.getRawJSON(message.getTweet());
		InputStream stream = new ByteArrayInputStream(rawJSON.getBytes(StandardCharsets.UTF_8));
		try (InputStream input = stream) {
			Files.copy(input, status.toPath());
		}

	}

}

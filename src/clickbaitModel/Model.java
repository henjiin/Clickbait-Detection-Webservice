package clickbaitModel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import corpus.builder.ProtobuffFeatureRecord.FeatureRecord;
import experiment.builder.ExperimentDescriptions.AttributeDescription;
import experiment.feature.Feature;
import experiment.feature.FeatureLists;
import featureRecords.RecordBuilder;
import message.Message;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.SparseInstance;

public class Model {
	List<Feature> features ;
	Instances dataSet;
	Classifier cls;
	String name = "default";

	public void setName(String name) {
		this.name = name;
	}

	public Model(String arffFile, List<Feature> features) throws Exception {
		this.dataSet = new Instances(new StringReader(arffFile));
		this.dataSet.setClass(dataSet.attribute("clickbait-class"));
		this.features = features;
	}

	public void useClassifier(Classifier classifier) {
		this.cls = classifier;
	}

	public void trainClassifier(Classifier classifier) throws Exception {
		System.out.println("Started Training Classifier");
		this.cls = classifier;
		cls.buildClassifier(dataSet);
		System.out.println("Ended Training Classifier");
	}

	public Response getpredictedClass(Message message) throws Exception {
		SparseInstance instance = buildInstance(message);
		double clsLabel = cls.classifyInstance(instance);
		Response response = new Response();
		response.setUsedModelName(name);
		response.setPredictedClass(instance.classAttribute().value((int) clsLabel));
		response.setMessageText(StringEscapeUtils.escapeHtml3(message.getText()).replace("…", "..."));		
		response.setId(message.getID());
		System.out.println(response.getMessageText());
		double[] predictions = cls.distributionForInstance(instance);
		for (int i = 0; i < predictions.length; i = i + 1) {			
			response.setPrediction(dataSet.classAttribute().value(i).replace("-", ""), predictions[i]);
		}
		if(message.isTweet()){
			response.setStatus(message.getTweet());
		}
		return response;
	}

	private SparseInstance buildInstance(Message message) throws IOException, FileNotFoundException {
		RecordBuilder recordBuilder = new RecordBuilder();
		FeatureRecord record = recordBuilder.returnRecord(features, message, FeatureRecord.newBuilder());
		SparseInstance instance = new SparseInstance(dataSet.numAttributes());
		instance.setDataset(dataSet);
		fillWithValues(record, instance);
		fillMissing(instance);
		return instance;
	}

	private void fillMissing(SparseInstance instance) {
		for (int i = 0; i < dataSet.numAttributes(); i++) {
			Attribute attribute = dataSet.attribute(i);
			if (instance.isMissing(attribute) && !attribute.name().equals("clickbait-class")) {
				if (attribute.isNominal()) {
				} else
					instance.setValue(attribute, 0);
			}
		}
	}

	private void fillWithValues(FeatureRecord record, SparseInstance instance) {
		for (corpus.builder.ProtobuffFeatureRecord.AttributeDescription f : record.getFeaturesList()) {
			for (int i = 0; i < dataSet.numAttributes(); i++) {
				Attribute attribute = dataSet.attribute(i);
				if (f.getName().equals(attribute.name())) {
					if (attribute.isNominal()) {
						instance.setValue(attribute, f.getValue());
					} else
						instance.setValue(attribute, Double.valueOf(f.getValue()));
				}
			}
		}
	}

}

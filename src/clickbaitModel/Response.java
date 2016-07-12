package clickbaitModel;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import twitter4j.Status;


public class Response {
	public Map<String, Double> classToProbability = new HashMap<String, Double>();
	String predictedClass="";
	String usedModelName ="default";
	String messageText="";
	Status status = null;
	String id = "-1";

	public Map<String, Double> getClassToProbability() {
		return classToProbability;
	}
	public void setClassToProbability(Map<String, Double> classToProbability) {
		this.classToProbability = classToProbability;
	}
	public String getPredictedClass() {
		return predictedClass;
	}
	public void setPredictedClass(String predictedClass) {
		this.predictedClass = predictedClass;
	}
	
	public void setPrediction(String className, double probability){
		classToProbability.put(className, probability);
	}
	public void getPrediction(String className){
		classToProbability.get(predictedClass);
	}
	
	public String toString(){
		return toJSONstring();
	}
	
	public String toJSONstring(){
		Gson gson = new Gson();
		String json = gson.toJson(this);
		return json;
	}
	public String getUsedModelName() {
		return usedModelName;
	}
	public void setUsedModelName(String usedModelName) {
		this.usedModelName = usedModelName;
	}
	public String getMessageText() {
		return messageText;
	}
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}
	public Status getResponse() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public String getId() {
		return id;
	}
	public void setId(String string) {
		this.id = string;
	}
}


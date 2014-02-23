package com.philipoy.picss.model;

public class Picss {
	
	public byte[] photo;
	public byte[] sound=null;
	public String name;
	public String label="";
	public String soundUrl="";
	
	public boolean isReady() {
		return (photo != null &&
				(sound != null || !soundUrl.equals("")) &&
				name!= null &&
				!name.trim().equals(""));
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("****************\n");
		b.append("* name : ").append(name).append("\n");
		b.append("* label : ").append(label).append("\n");
		b.append("* photo : ").append(photo!=null).append("\n");
		b.append("* sound : ").append(sound!=null).append("\n");
		b.append("* url : ").append(soundUrl).append("\n");
		b.append("* ready : ").append(isReady()).append("\n");
		b.append("****************");
		return b.toString();
	}
	
	
	
}

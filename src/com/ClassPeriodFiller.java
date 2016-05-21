package com;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.ArrayList;

public class ClassPeriodFiller {
	
	static Map<String, Integer> field;
	static ArrayList<ArrayList<String>> studentData;
	static File outFile;
	
	public ClassPeriodFiller() {
		Scanner file = null;
		try {
			file = new Scanner(new File("SMCS10_noGrades.mer"));
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		//create Map of Fields (line 1)
		field = new HashMap<String,Integer>();
		int i = 0;
		for(String fieldName: file.nextLine().split(",")){
			field.put(fieldName.trim(), i++);
		}
		
		//Load in student data
		studentData = new ArrayList<ArrayList<String>>();
		while(file.hasNext()){
			ArrayList<String> student = new ArrayList<String>();
			for(String studentField: file.nextLine().split("\",\"")){
				student.add(studentField.trim());
			}
			studentData.add(student);
		}
		
	}
	
	public ArrayList<ArrayList<ClassPeriod>> fillPeriods() {
		ArrayList<ArrayList<ClassPeriod>> periods = new ArrayList<ArrayList<ClassPeriod>>();
		for (int i = 0; i < 8; i++) {
			periods.add(new ArrayList<ClassPeriod>());
		}
		for (ArrayList<String> student : studentData) {
			int period = Integer.parseInt(student.get(field.get("PD")));
			String room = student.get(field.get("RMNO"));
			String firstName = student.get(field.get("TCHF"));
			String lastName = student.get(field.get("TCHL"));
			int grade = Integer.parseInt(student.get(field.get("GR")));
			ArrayList<ClassPeriod> classPeriods = periods.get(period - 1);
			boolean found = false;
			for (ClassPeriod classPeriod : classPeriods)
				if (classPeriod.toString().equals(firstName + " " + lastName)) {
					classPeriod.incrementClassSize(grade);
					found = true;
					break;
				}
			if (!found) {
				ClassPeriod newClass = new ClassPeriod(room, firstName, lastName);
				newClass.incrementClassSize(grade);
				classPeriods.add(newClass);
			}
		}
		return periods;
		
	}
	
}
/*******************************************************************************
 * Copyright 2014 Likya Teknoloji
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.likya.tlos.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.likya.tlos.LocaleMessages;
import com.likya.tlos.TlosServer;
import com.likya.tlos.jobs.Job;
import com.likya.tlos.jobs.RepetitiveExternalProgram;
import com.likya.tloslite.model.DependencyInfo;
import com.likya.tlos.model.JobProperties;
import com.likya.tlos.utils.loaders.ScenarioLoader;

public class DependencyOperations {
	
	public static ArrayList<String> getDependencyJobKeys(ArrayList<DependencyInfo> dependencyInfoList) {

		ArrayList<String> myList = new ArrayList<String>();

		Iterator<DependencyInfo> myIterator = dependencyInfoList.iterator();

		while (myIterator.hasNext()) {

			DependencyInfo dependencyInfo = myIterator.next();

			myList.add(dependencyInfo.getJobKey());
		}

		return myList;
	}

	public static ArrayList<Integer> getDependencyStatusList(ArrayList<DependencyInfo> dependencyInfoList) {

		ArrayList<Integer> myList = new ArrayList<Integer>();

		Iterator<DependencyInfo> myIterator = dependencyInfoList.iterator();

		while (myIterator.hasNext()) {

			DependencyInfo dependencyInfo = myIterator.next();

			myList.add(dependencyInfo.getStatus());
		}

		return myList;
	}

	public static ArrayList<Integer> getDependencyStatusList(String jobKey, ArrayList<DependencyInfo> dependencyInfoList) {

		ArrayList<Integer> myList = new ArrayList<Integer>();

		Iterator<DependencyInfo> myIterator = dependencyInfoList.iterator();

		while (myIterator.hasNext()) {

			DependencyInfo dependencyInfo = myIterator.next();
			if(dependencyInfo.getJobKey().equals(jobKey)) {
				myList.add(dependencyInfo.getStatus());
			}
		}

		return myList;
	}
	
	public static boolean hasDependentWithStatus(String jobKey, int status) {

		HashMap<String, Job> jobQueue = new HashMap<String, Job>(TlosServer.getJobQueue());

		// remove myself
		jobQueue.remove(jobKey);

		Iterator<Job> jobListIterator = jobQueue.values().iterator();

		while (jobListIterator.hasNext()) {

			Job tmpJob = jobListIterator.next();

			Iterator<DependencyInfo> dependentToMeListIterator = tmpJob.getJobProperties().getJobDependencyInfoList().iterator();

			while (dependentToMeListIterator.hasNext()) {
				DependencyInfo tmpDependencyInfo = dependentToMeListIterator.next();

				if (tmpDependencyInfo.getJobKey().equals(jobKey)) {
					if (getDependencyStatusList(tmpJob.getJobProperties().getJobDependencyInfoList()).indexOf(new Integer(status)) >= 0) {
						return true;
					}
				}
			}

		}

		return false;
	}

	public static ArrayList<Job> getDependencyList(HashMap<String, Job> jobQueue, Object jobKey) {

		ArrayList<Job> jobList = new ArrayList<Job>();

		try {
			Iterator<Job> jobsIterator = jobQueue.values().iterator();
			while (jobsIterator.hasNext()) {
				Job scheduledJob = jobsIterator.next();
				ArrayList<String> dependentJobList = DependencyOperations.getDependencyJobKeys(scheduledJob.getJobProperties().getJobDependencyInfoList());
				int indexOfJob = dependentJobList.indexOf(jobKey);
				if (indexOfJob > -1) {
					jobList.add(scheduledJob);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jobList.size() == 0 ? null : jobList;

	}
	
	public static boolean checkCyclicDependency() {
		
		HashMap<String, Job> jobQueue = TlosServer.getJobQueue();
		
		try {
			Iterator<Job> jobsIterator = jobQueue.values().iterator();
			
			while (jobsIterator.hasNext()) {
				Job scheduledJob = jobsIterator.next();
				ArrayList<String> dependentToJobList = DependencyOperations.getDependencyJobKeys(scheduledJob.getJobProperties().getJobDependencyInfoList());
				TlosServer.getLogger().warn("         >> " + scheduledJob.getJobProperties().getKey());
				if(recurseInToCycle(scheduledJob, dependentToJobList)) {
					return true;
				}
			}
			
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private static boolean recurseInToCycle(Job scheduledJob, ArrayList<String> dependentToJobList) {
		 
		if(dependentToJobList.indexOf(scheduledJob.getJobProperties().getKey().toString()) >= 0) {
			return true;
		} else {
			Iterator<String> jobsIterator = dependentToJobList.iterator();
			while(jobsIterator.hasNext()) {
				String recurJobKey = jobsIterator.next(); 
				if(recurJobKey != null && recurJobKey.equals(ScenarioLoader.UNDEFINED_VALUE)) {
					continue;
				}
				Job recurJob = 	TlosServer.getJobQueue().get(recurJobKey);
				ArrayList<String> tmpDependentToJobList = DependencyOperations.getDependencyJobKeys(recurJob.getJobProperties().getJobDependencyInfoList());
				TlosServer.getLogger().warn("  Analyzing dependency list of          >> " + recurJob.getJobProperties().getKey());
				if(recurseInToCycle(scheduledJob, tmpDependentToJobList)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static boolean validateDependencyList(Logger schedulerLogger, HashMap<String, Job> jobQueue) {

		// For Test
		Date startTime = Calendar.getInstance().getTime();
		boolean cyclCheck = checkCyclicDependency();
		String duration = "" + DateUtils.getDurationNumeric(startTime);
		TlosServer.getLogger().warn("Cyclicdependency checkduration : " + duration);
		
		if(cyclCheck) {
			schedulerLogger.info("Cyclic dependency resloved !");/*LocaleMessages.getString("ScenarioLoader.441")); //$NON-NLS-1$*/
			return false;
		}
		Iterator<Job> jobsIterator = jobQueue.values().iterator();

		while (jobsIterator.hasNext()) {
			
			Job scheduledJob = jobsIterator.next();
			
			ArrayList<DependencyInfo> dependentJobList = scheduledJob.getJobProperties().getJobDependencyInfoList();

			if(getDependencyJobKeys(dependentJobList).indexOf(scheduledJob.getJobProperties().getKey()) >= 0) {
				schedulerLogger.info(scheduledJob.getJobProperties().getKey() + LocaleMessages.getString("ScenarioLoader.441")); //$NON-NLS-1$
				return false;
			}
			
			int i = 0;
			while (i < dependentJobList.size()) {
				String key = (dependentJobList.get(i)).getJobKey();
				
				if(key.equals(ScenarioLoader.UNDEFINED_VALUE)) {
					++i;
					continue;
				}
				
				if ((!key.equals(ScenarioLoader.UNDEFINED_VALUE)) && (jobQueue.get(key) == null)) {
					schedulerLogger.info(scheduledJob.getJobProperties().getKey() + LocaleMessages.getString("ScenarioLoader.44") + key); //$NON-NLS-1$
					return false;
				}
				if (jobQueue.get(key) instanceof RepetitiveExternalProgram) {
					schedulerLogger.info(scheduledJob.getJobProperties().getKey() + LocaleMessages.getString("ScenarioLoader.19") + key); //$NON-NLS-1$
					return false;
				}
				
				if (jobQueue.get(key) .getJobProperties().isManuel()) {
					schedulerLogger.info(scheduledJob.getJobProperties().getKey() + LocaleMessages.getString("ScenarioLoader.191") + key); //$NON-NLS-1$
					return false;
				}
				
				// Her bir işin teker teker bağımlılık listesi alınır
				// Geçerli işin bağımlılık listesinde FAIL tipli bir bağımlılık tanımı var ise, bağlı olduğu iş bulunur.
				// Bağlı olunan iş non-blocker yapılır
				if(DependencyOperations.getDependencyStatusList(dependentJobList).indexOf(new Integer(JobProperties.FAIL)) >= 0) {
					// Sadece fail bağımlısı olduğu iş değil, bu iş de non-blocker olmalı.
					scheduledJob.getJobProperties().setBlocker(false);
					ArrayList<String> keyList = getDependencyJobKeys(dependentJobList);
					
					Iterator<String> keyListIterator = keyList.iterator();
					
					while(keyListIterator.hasNext()) {
						String tmpKey = keyListIterator.next();
						jobQueue.get(tmpKey).getJobProperties().setBlocker(false);
					}
					
				}

				++i;
			}
		}

		return true;

	}
}

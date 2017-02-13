package org.simplity.examples;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

public class QuartzJob implements Job {
        public void execute(JobExecutionContext context)
                        throws JobExecutionException {
        		ServiceData inData = new ServiceData();
        		inData.setUserId(Value.newTextValue("100"));
        		inData.setServiceName("helloworld");
        		System.out.println(ServiceAgent.getAgent().executeService(inData).getTrace());
        }
}
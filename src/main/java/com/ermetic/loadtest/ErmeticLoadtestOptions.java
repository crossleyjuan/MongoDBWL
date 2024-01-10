package com.ermetic.loadtest;

import org.apache.commons.cli.*;

//Yes - lots of public values, getters are OTT here.

public class ErmeticLoadtestOptions {

	String connectionDetails = "mongodb://localhost:27017";
	int duration = 5;
	int numThreads = 1;
	int contexts = 500;
	String contextId = null;

	boolean helpOnly = false;
	
	ErmeticLoadtestOptions(String[] args) throws ParseException
	{
		CommandLineParser parser = new DefaultParser();
		
		Options cliopt;
		cliopt = new Options();
		cliopt.addOption("d","duration", true, "Duration in minutes. Default 5");
		cliopt.addOption("h", "help", false, "Show help");
		cliopt.addOption("t", "threads", true, "Number of threads. Default 1");
		cliopt.addOption("u","uri",true,"MongoDB connection details (default 'mongodb://localhost:27017' )");
		cliopt.addOption("c", "ctx", true, "Number of contexts to test. Default: 500");
		cliopt.addOption("i", "ctxId", true, "Context id");

		CommandLine cmd = parser.parse(cliopt, args);

		if (cmd.hasOption("d")){
			duration = Integer.parseInt(cmd.getOptionValue("d"));
		}

		if(cmd.hasOption("h"))
		{
			HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "LoadTestMain", cliopt );
            helpOnly = true;	
		}

		if (cmd.hasOption("i")) {
			contextId = cmd.getOptionValue("i");
		}

		if (cmd.hasOption("t")){
			numThreads = Integer.parseInt(cmd.getOptionValue("t"));
		}

		if (cmd.hasOption("c")){
			contexts = Integer.parseInt(cmd.getOptionValue("c"));
		}

		if(cmd.hasOption("u"))
		{
			connectionDetails = cmd.getOptionValue("u");
		}
	}
}

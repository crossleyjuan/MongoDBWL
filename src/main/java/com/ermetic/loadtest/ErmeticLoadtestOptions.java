package com.ermetic.loadtest;

import org.apache.commons.cli.*;

//Yes - lots of public values, getters are OTT here.

public class ErmeticLoadtestOptions {

	String connectionDetails = "mongodb://localhost:27017";
	int duration = 5;
	int numThreads = 1;
	int contexts = 500;
	int queryTargettingMaxRatio = 100000;
	String contextFile = null;

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
		cliopt.addOption("i", "ctxId", true, "Context file to include");
		cliopt.addOption("m", "maxRatio", true, "Max allowed Query targeting ratio (Docs scanned vs returned). Default: 100000");

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
			contextFile = cmd.getOptionValue("i");
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
		if (cmd.hasOption("m")) {
			queryTargettingMaxRatio = Integer.parseInt(cmd.getOptionValue("m"));
		}
	}
}

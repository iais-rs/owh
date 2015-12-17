package owh;

import rst.time;
import rst.textio.OutputLogger;
import rst.textio.Param;

public final class RunOWH
{

	public static final void main (final String[] args)
	{
		final Param param = new Param(args,OWH.class);
		final String name = OWH.class.getSimpleName();
		new OutputLogger(param.get("target.dir"),name+".out");

		final long starttime = time.startTime("running "+name+" ...",true);
		time.printCurrentDateAndTime(false);
		param.print(true);

		new OWH(param);

		System.out.println("\ndone");
		time.printCurrentDateAndTime(false);
		time.printRunningTime(starttime,false);
	}

}

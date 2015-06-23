package tangible;

//----------------------------------------------------------------------------------------

import java.util.Date;

//	Copyright � 2007 - 2012 Tangible Software Solutions Inc.
//	This class can be used by anyone provided that the copyright notice remains intact.
//
//	This class is used to simulate some .NET date members in Java.
//----------------------------------------------------------------------------------------
public final class DotNetToJavaDateHelper
{
	//------------------------------------------------------------------------------------
	//	This method replaces various .NET date instance properties, such as 'Hour'.
	//------------------------------------------------------------------------------------
	public static int datePart(int calendarDatePart, java.util.Date date)
	{
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(date);
		if (calendarDatePart == java.util.Calendar.MONTH)
			//Month in java.util.Calendar is 0-based, so add 1 to simulate .NET:
			return c.get(calendarDatePart) + 1;
		else
			return c.get(calendarDatePart);
	}

	//------------------------------------------------------------------------------------
	//	This method replaces the .NET static date method 'DaysInMonth'. We follow the
	//	.NET convention of using 1 to 12 for months, not 0 to 11 as is common in Java.
	//------------------------------------------------------------------------------------
	public static int daysInMonth(int year, int month)
	{
		//Month in java.util.Calendar is 0-based, so subtract 1:
		java.util.Calendar cal = new java.util.GregorianCalendar(year, month - 1, 1);
		return cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
	}

	//------------------------------------------------------------------------------------
	//	This method replaces the .NET static date property 'Today'.
	//------------------------------------------------------------------------------------
	public static Date today()
	{
		java.util.Calendar now = java.util.Calendar.getInstance();
		//Month in java.util.Calendar is 0-based, so add 1 to simulate .NET:
		return dateForYMDHMS(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1, now.get(java.util.Calendar.DATE), 0, 0, 0);
	}

	//------------------------------------------------------------------------------------
	//	Replaces the deprecated constructor of java.util.Date which takes a year, month,
	//	and day, and sets everything else to zero. We follow the .NET convention of
	//	using 1 to 12 for months, not 0 to 11 as is common in Java.
	//------------------------------------------------------------------------------------
	public static Date dateForYMD(int year, int month, int day)
	{
		return dateForYMDHMS(year, month, day, 0, 0, 0);
	}

	//------------------------------------------------------------------------------------
	//	Replaces the deprecated constructor of java.util.Date which takes a year, month,
	//	day, hour, minute, and second. We follow  the .NET convention of using 1 to 12
	//	for months, not 0 to 11 as is common in Java.
	//------------------------------------------------------------------------------------
	public static Date dateForYMDHMS(int year, int month, int day, int hour, int minute, int second)
	{
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.clear();
		//Month in java.util.Calendar is 0-based, so subtract 1:
		cal.set(year, month - 1, day, hour, minute, second);
		return cal.getTime();
	}
}
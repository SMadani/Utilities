import java.text.SimpleDateFormat

class Tag
{
	private static def NbTags = 0, ids = []
	protected final def IDn
	def Name, Description, Properties, Value, Target, Timestamp, Groups
    static def Common
	
	public Tag()
	{
		def unique = ++NbTags
        while (ids.contains(NbTags))
            unique++
        IDn = unique
        ids.add(IDn)
	}
	public Tag (unique)
	{
        if (unique > 0)
        {
            ++NbTags
            while (ids.contains(unique))
                unique++
        }
        else while (ids.contains(unique))
            unique--

        IDn = unique
        ids.add(IDn)
	}

	def setTimestamp()
	{
		Timestamp = System.currentTimeMillis()
	}

    static def getIDs()
    {
        return ids.toArray()
    }

	def String toString (exclude)
	{
		def str = ""
        exclude = exclude?.length() > 1 ? exclude.toUpperCase() : "no"

        if (!exclude.contains("ID"))
            str += "ID: "+IDn+", "
		if (Name?.length() > 0 && !exclude.contains("NAME"))
			str += "Name: \"$Name\", "
		if (Description?.length() > 0 && !exclude.contains("DESC"))
			str += "Description: \"$Description\", "
		if (Value != null && !exclude.contains("VAL"))
			str += "Value: $Value, "
        if (Target != null && !exclude.contains("TARG"))
            str += "Target: $Target, "
        if (Properties?.length() > 0 && !exclude.contains("PROP"))
            str += "Properties: \"$Properties\", "
        if (Timestamp != null && !exclude.contains("TIME") && !exclude.contains("TS"))
            str += "Time: ${(new Date(Timestamp).toString())}, "
        if (Common?.length() > 0  && !exclude.contains("SHARE") && !exclude.contains("COMMON"))
            str += "Common: \"$Common\", "
        if (Groups?.size() > 0 && !exclude.contains("GROUP"))
        {
            str += "Groups: \""
            (0..Groups.size()).each {i -> str += Groups.get(i)+";"}
            str += "\", "
        }
		if (str.length() > 4)
			str = str.substring (0,str.length()-2)
        return str;
	}

    void fromString (include)
    {
        def exists = {
            def start, end, n = System.getProperty("line.separator")
            if (include?.contains("$it: "))
            {
                start = include.indexOf("$it: ")+"$it: ".length()
                if (include.charAt(start) == '"')
                {
                    ++start
                    end = include.indexOf('"', start)
                }
                else
                    end = include.indexOf(',',start)
                if (end < start || (end-start > include.indexOf(n,end) && include.indexOf(n,end) > start))
                {
                    end = include.indexOf(n, start)
                    if (end < start)
                        end = include.length()
                }
                include.substring start, end
            }
            else
                null
        }

        if (exists("Name") != null)
            Name = exists("Name")
        if (exists ("Description") != null)
            Description = exists ("Description")
        if (exists("Value") != null)
            Value = exists ("Value") as double
        if (exists("Target") != null)
            Target = exists ("Target") as double
        if (exists ("Properties") != null)
            Properties = exists ("Properties")
        if (exists("Time") != null)
            Timestamp = new SimpleDateFormat("EEE MMM d HH:mm:ss z YYYY").parse((String)exists("Time")).getTime() as long
        if (exists("Common") != null)
            Common = exists("Common")

        def x = 0, grs = (String)exists("Groups")
        if (grs?.length() > 0)
            while (grs.indexOf(';',x) >= 0)
            {
                if (grs.indexOf(';',x+1) >= 0)
                    Groups.add (grs.substring(x,include.indexOf(';',x+1)))
                x += grs.substring(grs.substring(x,include.indexOf(';',x+1))).length()
            }
    }
}

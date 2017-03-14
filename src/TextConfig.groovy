import java.nio.file.*
import java.nio.charset.Charset
import java.util.regex.Pattern

class TextConfig
{
	def Fields = []
    private def raw = "", dir, fconn
    private static final def n = System.getProperty ("line.separator")

    TextConfig() {}

    TextConfig (fileName)
    {
        if (!fileName)
        {
            def home, hs = this.hashCode()
            if (System.getProperty("file.separator").equals("\\"))
            {
                home = System.getProperty("user.home")
                home = home.replace('\\', '/')
                home += "/Desktop/$hs"
            }
            else
                home = System.getProperty("user.home")+"/Desktop/$hs"

            dir = Paths.get (URI.create("file:///${home}.txt"))
            fconn = dir.toFile()
        }
        else if (fileName instanceof TextConfig)
        {
            fconn = fileName.getFile()
            def path = fconn.getPath().replace('\\','/').replace(' ',"%20")
            dir = Paths.get (URI.create("file:///$path"))
        }
        else if (fileName instanceof String)
        {
            fileName = fileName.replace('\\', '/')
            if (getExtension(fileName).length() < 1)
                fileName += ".txt"
            if (fileName?.startsWith("file://"))
                dir = Paths.get(URI.create(fileName))
            else if (fileName?.contains('/'))
                dir = Paths.get(URI.create("file:///$fileName"))
            else
            {
                def home
                if (System.getProperty("file.separator").equals("\\"))
                {
                    home = System.getProperty("user.home")
                    home = home.replace('\\', '/')
                    home += "/Desktop/$fileName"
                }
                else
                    home = System.getProperty("user.home")+"/Desktop/$fileName"

                dir = Paths.get(URI.create("file:///$home"))
            }
            fconn = dir.toFile()
        }
        else if (fileName instanceof Path)
        {
            dir = fileName
            fconn = dir.toFile()
        }
        else if (fileName instanceof URI)
        {
            dir = Paths.get(fileName)
            fconn = dir.toFile()
        }
        else if (fileName instanceof File)
        {
            fconn = fileName
            dir = fconn.toPath()
        }
        else
        {
            fconn = new File(fileName.toString())
            dir = fconn.toPath()
        }
        if (fconn.exists())
        {
            this.update()
            def x = 0
            while (raw.indexOf(n, x) >= 0)
            {
                if (raw.indexOf(':', x) < 1)
                    break
                while (raw.substring(x, x+1).equals("\r") || raw.substring(x, x+1).equals("\n"))
                    ++x
                def field = raw.substring(x, raw.indexOf(':', x))
                Fields?.add(field)
                x += raw.indexOf(n, x)
            }
        }
        else
            fconn.createNewFile()
    }

    def setPath (fileName)
    {
        if (fileName instanceof String)
        {
            dir = Paths.get(URI.create(fileName))
            fconn = dir.toFile()
        }
        else if (fileName instanceof URI)
        {
            dir = fileName
            fconn = dir.toFile()
        }
        else if (fileName instanceof File)
        {
            fconn = fileName
            dir = fileName.toPath()
        }
    }

    File getFile()
    {
        fconn as File
    }

    def deleteFile()
    {
        fconn.delete()
    }

    def eraseContents()
    {
        def temp = raw
        raw = ""
        save()
        raw = temp
    }
    
    final def update()
    {
		if (fconn?.exists())
		{
            BufferedReader bfr = Files.newBufferedReader (dir, Charset.forName("UTF-8"));
            String line;
            raw = ""
            while ((line = bfr.readLine()) != null)
                raw += (line+n);
            bfr.close()
		}
    }
    
    final synchronized def save()
    {
        def bfr = Files.newBufferedWriter (dir, Charset.forName("UTF-8"))
		bfr.write (raw,0,raw.length())
		bfr.close()
    }

    String read()
    {
        raw as String
    }

    def overwrite (String text)
    {
        raw = text
    }

    boolean isBlankLine (int lineNumber)
    {
        def line = readLine(lineNumber)
        line == null ? null : !(line.length() > 0 && !line.equals("\r") && !line.equals("\n") && !line.equals("\r\n") && !line.equals("\n\r") && line.trim().length() > 0)
    }

    int getLastNonBlankLine (int fromLine)
    {
        def nol = numberOfLines
        if (fromLine < 0 || fromLine > nol)
            return -2
        for (int ln = nol; ln >= fromLine; ln--)
            if (!isBlankLine(ln))
                return ln
        -1
    }

    int getLastNonBlankLine()
    {
        getLastNonBlankLine(0)
    }

    int getNumberOfLines (boolean includeBlank)
    {
        def lines = 0, scanner = new Scanner (raw as String)
        for (def j = 1; scanner.hasNextLine(); scanner.nextLine())
        {
            if (!includeBlank && isBlankLine(lines+j))
                ++j
            else
                ++lines
        }
        scanner.close()
        lines as int
    }

    int getNumberOfNonBlankLines()
    {
        getNumberOfLines(false)
    }

    int getNumberOfLines()
    {
        getNumberOfLines(true)
    }

    int getLineOfField (String field)
    {
        //def ls = "$field: ${readValue(field)}"
        def line = getLinesContaining ("$field: ")
        line.size() > 0 ? line[0] : -1
    }

    String getFieldAtLine (int lineNumber)
    {
        def line = readLine(lineNumber)
        line.contains(':') ? line.substring (0,line.indexOf(':')) : ""
    }

    int[] getLinesContaining (expr, int fromLine)
    {
        def nol = numberOfLines, lines = []
        if (expr instanceof String)
        {
            for (i in fromLine..nol)
                if (readLine(i).contains(expr))
                    lines.add(i)
        }
        else if (expr instanceof Pattern)
        {
            for (i in fromLine..nol)
                if (expr.matcher (readLine(i)).find())
                    lines.add(i)
        }
        lines.toArray (new Integer[lines.size()]) as int[]
    }

    int[] getLinesContaining (expr)
    {
        getLinesContaining (expr,0)
    }

    String[] getLinesAsArray (int fromLine)
    {
        def nol = numberOfLines, lines = []
        if (fromLine >= 0 && fromLine < nol)
            for (int ln = fromLine; ln < nol; ln++)
                lines.add(readLine(ln))
        lines.toArray(new String[lines.size()])
    }

    String[] getLinesAsArray()
    {
        getLinesAsArray(0)
    }

    String getFieldFor (String expr)
    {
        def line = getLinesContaining(expr)
        line == null || line.length == 0 ? "" : getFieldAtLine(line[0])
        //assert readLine(ln).contains (readValue(field))
    }

    String readLine (int lineNumber)
    {
        if (lineNumber == 0)
            lineNumber = 1
        if (lineNumber <= numberOfLines && numberOfLines > 1 && lineNumber > 0)
        {
            def line = "", scanner = new Scanner (raw as String)
            for (it in (1..lineNumber))
            {
                if (scanner.hasNextLine())
                    line = scanner.nextLine()
                else
                {
                    line = ""
                    while (scanner.hasNext())
                        line += scanner.next()
                }
            }
            scanner.close()
            line
        }
        else
            raw
    }

    String readNextLineThatMatches (String pattern, int fromLine)
    {
        def nol = numberOfLines
        if (fromLine.equals(0))
            fromLine = 1
        if (fromLine > 0 && fromLine <= nol)
            for (int i = fromLine; i <= nol; i++)
                if (readLine(i).matches(pattern))
                    return readLine(i)
        return null
    }

    String readNextNonBlankLine (int fromLine)
    {
        def nol = numberOfLines
        if (fromLine.equals(0))
            fromLine = 1
        if (fromLine > 0 && fromLine <= nol)
            for (int i = fromLine; i <= nol; i++)
                if (!isBlankLine(i))
                    return readLine(i)
        return ""
    }

    String[] readAllNonBlankLines()
    {
        def lines = [], nol = numberOfLines
        for (int i = 1; i <= nol; i++)
            if (!isBlankLine(i))
                lines.add (readLine(i))
        lines.toArray (new String [lines.size()])
    }

    String readLastNonBlankLine()
    {
        def nol = numberOfLines
        for (int i = nol; i > 1; i--)
            if (!isBlankLine(i))
                return readLine(i);
        return "";
    }

    String[] readLinesThatContain (String term, int fromLine)
    {
        def lines = [], nol = numberOfLines
        if (fromLine <= 1)
            return readLinesThatContain(term)
        else if (fromLine > 0 && fromLine <= nol)
            for (int i = fromLine; i <= nol; i++)
            {
                def line = readLine(i)
                if (line.contains(term))
                    lines.add(line)
            }
        lines.toArray(new String[lines.size()])
    }

    String[] readLinesThatContain (String term)
    {
        def nol = numberOfLines, lines = []
        for (i in 1..nol)
        {
            def line = readLine(i)
            if (line.contains(term))
                lines.add(line)
        }
        lines.toArray(new String[lines.size()])
    }

    String readValue (String field)
    {
        def start = raw.indexOf(field)
        if (start < 0)
            return null
        else
            start += field.length()+1
        def end = raw.indexOf (n, start)
        if (end < 0)
            raw.substring (start)
        else
            raw.substring (start, end)
    }
    
    def writeValue (String field, String val)
    {
        def start = raw.indexOf(field)
        if (start < 0)
        {
            if (!raw.isEmpty())
                raw += n
            raw += "$field: $n"
            start = raw.indexOf(field)
            Fields.add(field)
        }
        start += field.length()+2
        def end = raw.indexOf (n,start)
        if (end < start)
        {
            raw += n
            end = raw.indexOf (n,start)
        }
        def before = raw.substring (0, start)
        def after = raw.substring (end, raw.length())
        raw = before + val + after
    }

    def remove (String field)
    {
        Fields.remove(field)
        deleteLine(getLineOfField(field))
    }

    def deleteLine (int lineNumber)
    {
        def line = readLine (lineNumber)
        def before = raw.substring (0,raw.indexOf(line))
        def after = raw.substring (before.length()+line.length())
        raw = before + after
    }

    def findAndReplace (what, with)
    {
        raw.replaceAll (what, with)
    }

    def append (String text, int lineNumber, int position)
    {
        def index = 0
        if (lineNumber == 0)
            lineNumber = 1
        assert (lineNumber <= numberOfLines && lineNumber > 0 && position <= readLine(lineNumber).length())
        for (it in (1..lineNumber))
            index = raw.indexOf(n, index)
        index += position
        if (index > raw.length())
            index = raw.length()
        def before = raw.substring (0,index)
        def after = raw.substring (index)
        raw = before + text + after
    }

    def append (String text, int lineNumber)
    {
        append (text, lineNumber, readLine(lineNumber).length()+1)
    }

    def append (String text)
    {
        //append (text, numberOfLines)
        raw += text
    }

    def insertNewLines (int lines)
    {
        for (it in (0..lines))
            append(n)
    }

    def insertNewLineAt (int lineNumber)
    {
        if (lineNumber <= numberOfLines)
            append(n, lineNumber)
        else
            insertNewLines(1)
    }

    String toJSON (String field, String property)
    {
        "{\"fieldName\":\"$field\", \"$property\": \"${readValue(field)}\"}"
    }
    
    String toJSON (String field)
    {
        "\"$field\": \"${readValue(field)}\""
    }
    
    String toJSON()
    {
        def arr = Fields.toArray(new String[0])
        def format = "{"
        for (f in 0..arr.length)
            format += "\"${arr[f]}\": \"${readValue(arr[f])}\","
        format += "\"${arr[arr.length-1]}\": \"${readValue(arr[arr.length-1])}\" }"
        return format
    }

    String toCSV()
    {
        def (fheader,content) = "";
        for (String field : fieldsAsArray)
        {
            fheader += "$field,"
            content += "${readValue(field)},"
        }
        fheader+n+content
    }

    String find (String expr, int occurN, int after) throws StringIndexOutOfBoundsException
    {
        def srch = "", index = 0
        for (i in 0..<occurN)
        {
            index = raw.indexOf(expr,index)
            if (index < 0)
                break
            index += expr.length()
            srch = raw.substring (index, index+after)
        }
        return srch
    }

    Set<String> find (Pattern regex)
    {
        def result = new HashSet<String>()
        def match = regex.matcher(raw)
        while (match.find())
            result.add (raw.substring(match.start(), match.end()))
        result as Set<String>
        //regex.matcher(raw).findAll().toSet()
    }

    Set<String> find (expression, terminalRegex)
    {
        def end, start = 0, output = []
        while (start >= 0)
        {
            start = expression.is(String) ? raw.indexOf (expression, start) : expression.matcher (raw.substring(start)).start();
            if (start < 0) break
            if (terminalRegex.is(Pattern))
            {
                try
                {
                    end = terminalRegex.matcher(raw.substring(start)).end()
                } catch (IllegalThreadStateException ignored)
                {
                    continue
                }
            }
            else end = raw.indexOf (terminalRegex, start)
            if (end < 0) break
            output.add (raw.substring (start, start+end))
        }
        output.toSet()
    }

    static String getExtension (String fileName)
    {
        def i = fileName.lastIndexOf('.')
        def p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'))
        return i > p ? fileName.substring(i+1) : ""/*fileName.substring(fileName.lastIndexOf('.'), fileName.length())*/
    }

    String getExtension()
    {
        getExtension(file.getName())
    }

    Object[] getFieldsAsArray()
    {
        Fields.toArray(new Object[Fields.size()])
    }

}

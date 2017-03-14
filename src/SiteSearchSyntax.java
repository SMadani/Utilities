import java.net.URL;

public interface SiteSearchSyntax
{
    URL query (String itemName);
    String getItemName (URL query);
}

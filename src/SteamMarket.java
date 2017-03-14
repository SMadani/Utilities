import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;

public class SteamMarket
{
    static String website = "http://steamcommunity.com/market", app = "Steam_Market_prices";
    static SiteSearchSyntax sq = new SiteSearchSyntax()
    {
        @Override
        public URL query(String itemName)
        {
            String query = website+"/search?q=";
            itemName = itemName.toLowerCase().replace(' ', '+');
            try
            {
                return new URL(query+itemName);
            } catch (MalformedURLException mue)
            {
                JFrame popup = new JFrame(app);
                popup.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                JOptionPane.showMessageDialog(popup, "The website is not a valid URL", "Invalid site", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        @Override
        public String getItemName (URL query)
        {
            char[] array = query.toString().substring ((website+"/search?q=").length()).replace('+',' ').toCharArray();
            array[0] = Character.toUpperCase(array[0]);
            for (int c = 1; c <= array.length; c++)
                if (array[c-1] == ' ' || array[c-1] == '-')
                    array[c] = Character.toUpperCase(array[c]);
            return new String(array);
        }
    };

    public static void main(String[] args)
    {
        if (args.length > 0) app = args[0];
        if (args.length > 1) website = args[1];
        String interval = args.length > 2 ? args[2] : "12";
        String currency = args.length > 3 ? args[3] : "USD GBP";
        double deviation = args.length > 4 ? Double.parseDouble(args[4]) : -0.15;
        long delay = args.length > 5 ? Long.parseLong(args[5]) : 3350;
        String location = args.length > 6 ? args[6] : "\"Starting at:\",1,50";
        try {
            new ChangeNotifier(app, website, sq, location, currency, interval, deviation, delay).start(true);
        }
        catch (NoClassDefFoundError error)
        {
            JFrame popup = new JFrame(app);
            popup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JOptionPane.showMessageDialog(popup, "Please ensure that \"groovy-all\".jar is in the same location as this program and restart the application.", "Initialization error", JOptionPane.ERROR_MESSAGE);
        } /*catch (RuntimeException error) {
            JFrame popup = new JFrame(app);
            popup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JOptionPane.showMessageDialog(popup, error, "Oops! Something went wrong...", JOptionPane.ERROR_MESSAGE);
        }*/
    }
}

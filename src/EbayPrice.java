import javax.swing.JOptionPane;
import javax.swing.JFrame;

public class EbayPrice
{
    public static void main(String[] args)
    {
        String app = args.length > 0 ? args[0] : "eBay_Price_Notifier";
        String website = args.length > 1 ? args[1] : "http://www.ebay.co.uk";
        String interval = args.length > 2 ? args[2] : "12";
        String currency = args.length > 3 ? args[3] : null;
        double deviation = args.length > 4 ? Double.parseDouble(args[4]) : 0.01;
        long delay = args.length > 5 ? Long.parseLong(args[5]) : 2000;
        String location = args.length > 6 ? args[6] : "\"<span class=\"notranslate\" id=\"prcIsum_bidPrice\" itemprop=\"price\">\",1,8";
        try {
            new ChangeNotifier(app, website, null, location, currency, interval, deviation, delay).start(true);
        }
        catch (NoClassDefFoundError error)
        {
            JFrame popup = new JFrame(app);
            popup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JOptionPane.showMessageDialog(popup, "Please ensure that \"groovy-all\".jar is in the same location as this program and restart the application.", "Initialization error", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException error) {
            JFrame popup = new JFrame(app);
            popup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JOptionPane.showMessageDialog(popup, error, "Oops! Something went wrong...", JOptionPane.ERROR_MESSAGE);
        }
    }
}

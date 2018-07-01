package products;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Base64;

public class GenerateXML {
    private final String CENEO_BASE_LINK = "https://www.ceneo.pl/";
    private final int WRITE_OFF = 0;

    // utility method to create text node
    private Node getProductsElements(org.w3c.dom.Document doc, org.w3c.dom.Element element, String name, String value) {
        org.w3c.dom.Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }

    public byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, WRITE_OFF, len);
        }
        return os.toByteArray();
    }

    private byte[] getBase64EncodedImage(String imageURL) throws IOException {
        java.net.URL url = new java.net.URL(imageURL);
        InputStream is = url.openStream();
        return getBytesFromInputStream(is);
    }

    private Node getImage(org.w3c.dom.Document doc, org.w3c.dom.Element element, String name, String value) {
        try {
            byte[] imageBytes = getBase64EncodedImage(value);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            org.w3c.dom.Element node = doc.createElement(name);
            node.appendChild(doc.createTextNode(base64));
            return node;
        } catch (IOException e) {
            org.w3c.dom.Element node = doc.createElement(name);
            node.appendChild(doc.createTextNode("Image not found"));
            return node;
        }
    }

    private Node getProduct(org.w3c.dom.Document doc, String name, String price, String img) {
        org.w3c.dom.Element product = doc.createElement("Product");
        product.appendChild(getProductsElements(doc, product, "Name", name));
        product.appendChild(getProductsElements(doc, product, "Price", price));
        product.appendChild(getImage(doc, product, "Image", img));
        return product;
    }

    private String getCurrentImg(Element name) {
        if (!name.select("img").first().absUrl("data-original").isEmpty()) {
            return name.select("img").first().absUrl("data-original");
        } else {
            return name.select("img").first().absUrl("src");
        }
    }

    public GenerateXML(String link) {
        try {
            final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.15 (KHTML, like Gecko) Chrome/24.0.1295.0 Safari/537.15";
            Document doc = Jsoup.connect(link).userAgent(USER_AGENT).timeout(5000).get();
            DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder icBuilder;
            icBuilder = icFactory.newDocumentBuilder();
            org.w3c.dom.Document xmlDoc = icBuilder.newDocument();
            org.w3c.dom.Element mainRootElement = xmlDoc.createElementNS(link, "Products");
            xmlDoc.appendChild(mainRootElement);

            String currentTitle = null;
            String prevTitle = null;
            String currentPrice = null;
            String currentImg = null;
            do {
                Elements names = doc.getElementsByClass("go-to-product");
                for (Element name : names) {
                    String title = name.attr("title");
                    if (title.isEmpty()) {
                        if (name.getElementsByAttribute("alt").first() != null) {
                            title = name.getElementsByAttribute("alt").first().attr("alt");
                        }
                    }
                    if (title != null && !title.isEmpty()) {
                        currentTitle = title;
                    }

                    if (!currentTitle.equals(prevTitle) && currentPrice != null) {
                        prevTitle = currentTitle;
                        mainRootElement.appendChild(getProduct(xmlDoc, prevTitle, currentPrice, currentImg));
                        currentPrice = null;
                        currentImg = null;
                    }
                    String price = name.getElementsByClass("price-format").text();
                    if (price != null && !price.isEmpty()) {
                        currentPrice = price;
                    }


                    if (name.select("img").first() != null) {
                        currentImg = getCurrentImg(name);
                    }

                }
                Elements lin = doc.getElementsByAttributeValue("rel", "next");

                if (lin != null && !lin.isEmpty()) {
                    String link_addr = CENEO_BASE_LINK + lin.attr("href");
                    doc = Jsoup.connect(link_addr).userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                            .referrer("http://www.google.com").timeout(5032).get();
                } else {
                    break;
                }
            } while (true);

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(xmlDoc);
            StreamResult file = new StreamResult(new File("ceneo_products.xml"));
            transformer.transform(source, file);
            System.out.println("XML file saved as ceneo_products.xml");
        } catch (IOException e) {
            System.out.println("Bad link");
        } catch (ParserConfigurationException e) {
            System.out.println("Something went wrong");
        } catch (TransformerException e) {
            System.out.println("Something went wrong");
        } catch (IllegalArgumentException e) {
            System.out.println("Bad link");
        }
    }
}
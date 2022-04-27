package de.cccc.ray.dupes;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static final String NAME = "name";
    private static final HashSet<Finding> foundDuplicates = new HashSet<>();

    public static void main(String[] args) {
        // Instantiate the Factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {

            // optional, but recommended
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();

            List<String> files = getXmlFilesInFolder(System.getProperty("user.dir"));

            System.out.println("\n## Scanning files for duplicates (Progress: each dot one file):");
            for (String fileName : files) {
                System.out.print(".");
                Document doc = db.parse(new File(fileName));
                // optional, but recommended
                // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
                doc.getDocumentElement().normalize();

                // get contents
                NodeList childNodes = doc.getDocumentElement().getChildNodes();

                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node node = childNodes.item(i);
                    if (node.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    Element element = (Element) node;
                    if (!element.hasAttribute(NAME)) {
                        // no name attribute? not interesting.
                        continue;
                    }

                    // alright, lets check all files for that tag name:
                    for (String otherFile : files) {
                        boolean isSameFile = otherFile.equals(fileName);
                        Document doc2 = isSameFile ? doc : db.parse(new File(otherFile));
                        // optional, but recommended
                        // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
                        doc2.getDocumentElement().normalize();

                        // get contents
                        NodeList childNodes2 = isSameFile ? childNodes : doc2.getDocumentElement().getChildNodes();
                        for (int j = 0; j < childNodes2.getLength(); j++) {
                            Node nodeToTest = childNodes2.item(j);
                            Node nameAttribute = null;
                            if (nodeToTest != null && nodeToTest.getAttributes() != null) {
                                nameAttribute = nodeToTest.getAttributes().getNamedItem(NAME);
                            }
                            if (node.getNodeType() != Node.ELEMENT_NODE || nameAttribute == null) {
                                continue;
                            }

                            if (!(isSameFile && node.isSameNode(nodeToTest)) && element.getAttribute(NAME).equals(nameAttribute.getNodeValue())) {
                                Finding finding = new Finding(element.getAttribute(NAME));
                                final Finding findingForSearch = finding;
                                List<Finding> found = foundDuplicates.stream().filter(findingForSearch::equals).collect(Collectors.toList());
                                if (found.size() > 0) {
                                    finding = found.get(0);
                                } else {
                                    foundDuplicates.add(finding);
                                }
                                finding.foundInFiles.add(fileName);
                            }
                        }
                    }
                }

            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        System.out.println("\n");
        if (foundDuplicates.isEmpty()) {
            System.out.println("+++ You're fine - no findings! +++");
        } else {
            System.out.println("### duplicate names found: ");
            for (Finding foundDuplicate : foundDuplicates) {
                System.out.println("Tag with name "+foundDuplicate.name+" found in Files:");
                for (String fileName : foundDuplicate.foundInFiles) {
                    System.out.println("    - "+fileName);
                }
            }
        }
    }

    private static List<String> getXmlFilesInFolder(String baseFolder) {
        System.out.println("## Scanning for XML files in folder: "+baseFolder);
        try (Stream<Path> walk = Files.walk(Paths.get(baseFolder))) {
            List<String> files = walk
                    .filter(p -> !Files.isDirectory(p))   // not a directory
                    .map(Path::toString)                  // convert path to string
                    .filter(f -> f.endsWith("xml"))       // check end with
                    .collect(Collectors.toList());// collect all matched to a List
            System.out.println("-> found "+files.size()+" XML files.");
            return files;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
        return null;
    }
}

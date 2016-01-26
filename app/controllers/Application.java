package controllers;

import org.w3c.dom.Document;
import play.libs.XPath;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Xml;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;


public class Application extends Controller {


    public Result index() {
        return ok(views.html.index.render());
    }

    public Result signin() {
        return ok(views.html.signin.render());
    }

    private Session session = null;
    private String iban = null;

    public Result initSession() {
        String sessionId = request().getQueryString("sessionId");
        String sessionIdSignature = request().getQueryString("sessionIdSignature");
        if (sessionId == null || sessionIdSignature == null)
            return internalServerError("No session parameters received.");
        session = new Session(sessionId, sessionIdSignature);
        return ok(views.html.controls.render());
    }


    public Result requestDefaultImport() {
        String additionalParams = "&since=2015-01-01"; // an arbitrary date...
        if (session == null)
            return redirect("/signin");
        else
            return requestCommand(Session.Command.DEFAULT_IMPORT, additionalParams);
    }


    public Result requestImportOwners() {
        if (session == null)
            return redirect("/signin");
        else
            return requestCommand(Session.Command.IMPORT_OWNERS, null);
    }

    public Result requestImportAccounts() {
        String additionalParams = "&fast=false"; // disable fast mode; fast=true is the default
        if (session == null)
            return redirect("/signin");
        else
            return requestCommand(Session.Command.IMPORT_ACCOUNTS, additionalParams);
    }

    public Result requestImportTransactions() {

        if (session == null) {
            return redirect("/signin");
        }
        else if (iban == null) {
            return notFound("You must fetch result for import accounts command first!");
        }
        else {
            String additionalParams = "&fast=false&since=2015-01-01"; // fast=false is the default
            additionalParams += iban;
            return requestCommand(Session.Command.IMPORT_TRANSACTIONS, additionalParams);
        }
    }

    private Result requestCommand(controllers.Session.Command command, String additionalParams) {
        Document xmlDoc;
        try {
            switch(command) {
                case DEFAULT_IMPORT:
                    xmlDoc = session.fetchResultForCommand(command, additionalParams);
                    break;
                case IMPORT_TRANSACTIONS:
                    xmlDoc = session.fetchResultForCommand(command, additionalParams);
                    break;
                case IMPORT_ACCOUNTS:
                    xmlDoc = session.fetchResultForCommand(command, additionalParams);
                    // now is the time to initialize the iban string; iban is
                    // an obligatory parameter for import transactions command
                    String value = XPath.selectText("//result/accounts/account/iban", xmlDoc);
                    iban = "&iban=" + value;
                    break;
                default:
                    xmlDoc = session.fetchResultForCommand(command, null);
            }


        } catch (Exception e) {
            e.printStackTrace(); // log exception
            return notFound("Can't load result. Something went wrong");
        }
        String xml = getStringFromDocument(xmlDoc);
        return ok(Xml.apply(xml));
    }

    private String getStringFromDocument(Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void writeXmlDoc(Document doc, String fileName) {
        File file = new File(fileName);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        DOMSource source = new DOMSource(doc);
        try {
            Transformer transformer = transformerFactory.newTransformer();
            StreamResult result = new StreamResult(file);
            transformer.transform(source, result);
        } catch (TransformerException e1) {
            System.out.println(this + ": " + e1.getMessage());
        }
    }


}

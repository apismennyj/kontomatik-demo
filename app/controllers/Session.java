package controllers;

import org.w3c.dom.Document;
import play.libs.F;
import play.libs.XPath;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by eduarddedu on 31/12/15.
 */
public class Session {
    public enum Command {
        DEFAULT_IMPORT,
        IMPORT_OWNERS,
        IMPORT_ACCOUNTS,
        IMPORT_TRANSACTIONS
    }
    private Map<Command, String> map = new HashMap<>();
    {
        map.put(Command.DEFAULT_IMPORT, "https://test.api.kontomatik.com/v1/command/default-import.xml");
        map.put(Command.IMPORT_OWNERS, "https://test.api.kontomatik.com/v1/command/import-owners.xml");
        map.put(Command.IMPORT_ACCOUNTS, "https://test.api.kontomatik.com/v1/command/import-accounts.xml");
        map.put(Command.IMPORT_TRANSACTIONS, "https://test.api.kontomatik.com/v1/command/import-account-transactions.xml");
    }
    private final String PARAMETERS;
    private ExecutorService service = Executors.newSingleThreadExecutor();


    public Session(String sessionId, String sessionIdSignature) {
        String apiKey = "YOUR_API_KEY_HERE";
        PARAMETERS = "apiKey=" + apiKey +
                "&sessionId=" + sessionId +
                "&sessionIdSignature=" + sessionIdSignature;
    }


    private final int millis = 5000;

    private WSResponse sendPostRequest(String url, String body) throws F.PromiseTimeoutException {
        WSRequest request = WS.url(url);
        request.setHeader("Content-type", "application/x-www-form-urlencoded");
        return request.post(body).get(millis);

    }

    private WSResponse sendGetRequest(String url) throws F.PromiseTimeoutException {
        WSRequest request = WS.url(url);
        request.setHeader("Content-type", "application/x-www-form-urlencoded");
        return request.get().get(millis);

    }


    public Document fetchResultForCommand(Command command, String additionalParams)
            throws TimeoutException, InterruptedException, ExecutionException {
        String url = map.get(command);
        String body = PARAMETERS;
        if (additionalParams != null) body += additionalParams;
        WSResponse response = sendPostRequest(url, body);
        try {
            return pollForDocument(response);
        } catch (F.PromiseTimeoutException e) {
            throw new TimeoutException(e.getMessage());
        }
    }



    private Document pollForDocument(WSResponse response) throws TimeoutException, InterruptedException, ExecutionException {
        String commandId = XPath.selectText("//@id", response.asXml());
        PollingTask task = new PollingTask(commandId);
        Future<Document> future = service.submit(task);
        try {
            return future.get(60, TimeUnit.SECONDS); // wait for 1 min, then give up
        } catch (TimeoutException ex) {
            System.out.println("Server timeout.");
            service.shutdownNow();
            task.stopped = true; // in case ExecutorService.shutdownNow() fails to stop the task
            throw ex;
        }
    }

    private class PollingTask implements Callable<Document> {
        volatile boolean stopped = false;
        String url;

        public PollingTask(String commandId) {
            url = "https://test.api.kontomatik.com/v1/command/" + commandId + ".xml";
            url += "?" + Session.this.PARAMETERS;
        }


        public Document call() {
            try {
                while (!stopped) {
                    Document document = sendGetRequest(url).asXml();
                    String state = XPath.selectText("//@state", document);
                    if (state.equals("successful")) {
                        return document;
                    }
                    TimeUnit.SECONDS.sleep(1);
                }
            } catch (InterruptedException ex) {
                System.out.println("PollingTask interrupted");
                return null;
            }
            return null;
        }
    }

}

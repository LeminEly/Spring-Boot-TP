package mr.supnum.bib.impl;

import jakarta.xml.ws.Endpoint;

/** Publie le service contract-first sur le serveur HTTP embarqué du JDK. */
public final class Serveur {

    private static final String URL = "http://localhost:8080/bib/EmpruntService";

    private Serveur() {
    }

    public static void main(String[] args) {
        Endpoint.publish(URL, new EmpruntServiceImpl());
        System.out.println("WSDL : " + URL + "?wsdl");
    }
}

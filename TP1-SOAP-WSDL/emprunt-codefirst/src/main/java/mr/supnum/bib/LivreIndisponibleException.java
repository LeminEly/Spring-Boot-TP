package mr.supnum.bib;

import jakarta.xml.ws.WebFault;

/** Fault métier déclarée dans le contrat : le livre demandé est déjà emprunté. */
@WebFault(name = "LivreIndisponible", targetNamespace = "http://supnum.mr/bib")
public class LivreIndisponibleException extends Exception {

    private static final long serialVersionUID = 1L;

    private final transient LivreIndisponibleInfo faultInfo;

    public LivreIndisponibleException(String message, LivreIndisponibleInfo faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /** Lu par JAX-WS pour construire l'élément {@code detail} de la Fault. */
    public LivreIndisponibleInfo getFaultInfo() {
        return faultInfo;
    }
}

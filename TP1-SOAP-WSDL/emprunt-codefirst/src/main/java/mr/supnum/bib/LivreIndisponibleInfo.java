package mr.supnum.bib;

/** Contenu du {@code detail} de la Fault « livre indisponible ». */
public class LivreIndisponibleInfo {

    public String isbn;
    public String retourPrevu;

    /** Requis par JAXB. */
    public LivreIndisponibleInfo() {
    }

    public LivreIndisponibleInfo(String isbn, String retourPrevu) {
        this.isbn = isbn;
        this.retourPrevu = retourPrevu;
    }
}

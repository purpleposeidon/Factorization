package factorization.api.datahelpers;

public enum Share {
    PRIVATE(false, false, false),
    VISIBLE(true, false, false),
    MUTABLE(true, true, false),
    PRIVATE_TRANSIENT(false, false, true),
    VISIBLE_TRANSIENT(true, false, true),
    MUTABLE_TRANSIENT(true, true, true),
    DESCRIPTION_PACKET(true, false, true);
    
    final public boolean is_public, client_can_edit, is_transient;
    
    Share(boolean pub, boolean mut, boolean tran) {
        is_public = pub;
        client_can_edit = mut;
        is_transient = tran;
    }
}
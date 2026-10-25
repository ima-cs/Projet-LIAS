package ma.fsbm.lias.module.auth;

/** Private delivery event, never serialized by a controller. */
public record InvitationCreated(String email, String token) {}

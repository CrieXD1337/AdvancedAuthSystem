package ru.rexlite.auth.providers;

public interface DataProvider {
    boolean isRegistered(String nick);
    boolean isPasswordCorrect(String nick, String hash);
    void register(String nick, String hash, String ip, String clientId, String email);
    void updatePassword(String nick, String newHash);
    String getClientId(String nick);
    void updateClientId(String nick, String clientId);
    int getAccountsByIp(String ip);
    String getPasswordHash(String nick);
    void close();
}

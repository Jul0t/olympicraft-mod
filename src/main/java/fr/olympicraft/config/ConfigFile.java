package fr.olympicraft.config;

public interface ConfigFile {

    int schemaVersion();

    void schemaVersion(int schemaVersion);

    void validate();
}

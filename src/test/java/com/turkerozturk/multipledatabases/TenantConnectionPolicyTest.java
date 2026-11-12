package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TenantConnectionPolicyTest {
    @Test void allowsAdminConfiguredLoopbackPrivateAndIpv6DatabaseEndpoints() {
        TenantConnectionPolicy.validate("org.postgresql.Driver","jdbc:postgresql://localhost:5432/notes?sslmode=require");
        TenantConnectionPolicy.validate("org.mariadb.jdbc.Driver","jdbc:mariadb://192.168.1.114:3306/notes");
        TenantConnectionPolicy.validate("com.mysql.cj.jdbc.Driver","jdbc:mysql://[::1]:3306/notes");
        TenantConnectionPolicy.validate("org.sqlite.JDBC","jdbc:sqlite:CTBDATA/demo.ctb");
        TenantConnectionPolicy.validate("org.sqlite.JDBC","jdbc:sqlite:file:CTBDATA/demo.ctb?mode=ro");
    }
    @Test void rejectsUnknownDriversMismatchesResourcesAndControlCharacters() {
        assertThatThrownBy(()->TenantConnectionPolicy.validate("example.UntrustedDriver","jdbc:evil:run"))
                .hasMessage("wizard.unsupportedDriver");
        for(String url:new String[]{null,"https://example.org/db","jdbc:mysql://localhost/db","jdbc:postgresql:","jdbc:postgresql://localhost/db\n"})
            assertThatThrownBy(()->TenantConnectionPolicy.validate("org.postgresql.Driver",url)).hasMessage("wizard.invalidUrl");
        assertThatThrownBy(()->TenantConnectionPolicy.pool("org.sqlite.JDBC","jdbc:sqlite::resource:https://example.org/db.ctb",null,null))
                .hasMessage("wizard.invalidUrl");
    }
    @Test void invalidReplacementCannotCloseTheExistingPoolOrWriteItsFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path folder) throws Exception {
        var old=org.mockito.Mockito.mock(ManagedTenantDataSource.class);
        var routing=new MultitenantDataSource();routing.setTargetDataSources(java.util.Map.of("Existing",old));routing.afterPropertiesSet();
        var configuration=new MultitenantConfiguration(){@Override public javax.sql.DataSource dataSource(){return routing;}};
        var file=java.nio.file.Files.writeString(folder.resolve("existing.txt"),"name=Existing\n");
        var properties=new java.util.Properties();properties.setProperty("name","Existing");
        properties.setProperty("datasource.driver-class-name","org.postgresql.Driver");
        properties.setProperty("datasource.url","jdbc:sqlite:other.ctb");
        assertThatThrownBy(()->configuration.installTenantConfig("Existing",file,new byte[0],properties,TenantWizardService.revision(file)))
                .hasMessage("wizard.invalidUrl");
        org.mockito.Mockito.verifyNoInteractions(old);
        assertThat(java.nio.file.Files.readString(file)).isEqualTo("name=Existing\n");
        assertThat(routing.getResolvedDataSources().get("Existing")).isSameAs(old);
    }
}

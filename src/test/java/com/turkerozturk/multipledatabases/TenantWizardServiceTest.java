package com.turkerozturk.multipledatabases;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import javax.sql.DataSource;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantWizardServiceTest {
    @TempDir Path directory;
    private MultitenantDataSource routing;
    private CustomPropertiesHolder holder;
    private TenantWizardService wizard;
    @BeforeEach void setup() {
        routing=new MultitenantDataSource();routing.setTargetDataSources(Map.of());routing.afterPropertiesSet();holder=new CustomPropertiesHolder();
        var configuration=new MultitenantConfiguration(){@Override public DataSource dataSource(){return routing;}};
        ReflectionTestUtils.setField(configuration,"customPropertiesHolder",holder);
        var tenants=mock(TenantService.class);
        when(tenants.getAllTenants()).thenAnswer(invocation->{Map<String,DataSource> map=new HashMap<>();routing.getResolvedDataSources().forEach((k,v)->map.put((String)k,v));return map;});
        wizard=new TenantWizardService(tenants,holder,configuration,directory.resolve("allTenants"));
    }
    @AfterEach void close(){routing.getResolvedDataSources().values().forEach(ds->{if(ds instanceof ManagedTenantDataSource m)m.close();});}
    private Path ctb() throws Exception {
        Path file=directory.resolve("Türkçe notlar.ctb");
        try(var input=getClass().getResourceAsStream("/fixtures/shared-node-tree.sql");var connection=DriverManager.getConnection("jdbc:sqlite:"+file);var statement=connection.createStatement()) {
            for(String line:new String(input.readAllBytes(),StandardCharsets.UTF_8).split("\\R"))if(line.startsWith("CREATE TABLE"))statement.execute(line);
        }return file;
    }
    private Path register(Properties p,ManagedTenantDataSource source) throws Exception {
        Path configs=Files.createDirectories(directory.resolve("allTenants"));Path file=configs.resolve("original.txt");
        try(var writer=Files.newBufferedWriter(file,StandardCharsets.UTF_8)){p.store(writer,"Keep this comment");}
        routing.setTargetDataSources(Map.of(p.getProperty("name"),source));routing.afterPropertiesSet();
        holder.addCustomProperties(p.getProperty("name"),Map.of("propertyFileName",file.getFileName().toString()));return file;
    }
    private Properties properties(Path file)throws Exception{var p=new Properties();try(var in=Files.newBufferedReader(file,StandardCharsets.UTF_8)){p.load(in);}return p;}
    private Properties existing(Path ctb){var p=new Properties();p.setProperty("name","Original");p.setProperty("datasource.url","jdbc:sqlite:"+ctb);p.setProperty("datasource.driver-class-name","org.sqlite.JDBC");p.setProperty("datasource.password","private-secret");p.setProperty("custom.future.setting","keep this");return p;}
    @Test void createsAndOpensInPlaceWithFilenameDefaultAndSafeDefaults()throws Exception {
        Path file=ctb();byte[] before=Files.readAllBytes(file);var form=new TenantWizardForm();form.path=file.toString();
        assertThat(wizard.save(form)).isEqualTo(file.getFileName().toString());
        var source=(ManagedTenantDataSource)routing.getResolvedDataSources().get(form.name);source.activate(true);source.close();
        assertThat(Files.readAllBytes(file)).isEqualTo(before);
        var name=holder.getCustomProperties(form.name).get("propertyFileName");var p=properties(directory.resolve("allTenants").resolve(name));
        assertThat(p.getProperty("datasource.url")).isEqualTo("jdbc:sqlite:"+file);
        assertThat(p.getProperty("custom.isWritable")).isEqualTo("false");assertThat(p.getProperty(CtbSchemaCompatibility.SETTING)).isEqualTo("false");
    }
    @Test void editRetainsUnknownPropertiesAndPasswordWithoutDisplayingIt()throws Exception {
        var old=mock(ManagedTenantDataSource.class);Path config=register(existing(ctb()),old);
        var form=wizard.edit("Original");assertThat(form.password).isEmpty();form.name="Renamed";form.writable=true;
        wizard.save(form);var p=properties(config);
        assertThat(p.getProperty("custom.future.setting")).isEqualTo("keep this");assertThat(p.getProperty("datasource.password")).isEqualTo("private-secret");
        assertThat(p.getProperty("custom.isWritable")).isEqualTo("true");assertThat(routing.getResolvedDataSources()).containsKey("Renamed").doesNotContainKey("Original");verify(old).close();
        form=wizard.edit("Renamed");form.clearPassword=true;wizard.save(form);assertThat(properties(config).getProperty("datasource.password")).isNull();
    }
    @Test void staleFormAndBusyPoolNeverOverwriteConfig()throws Exception {
        var old=mock(ManagedTenantDataSource.class);Path config=register(existing(ctb()),old);var form=wizard.edit("Original");
        Files.writeString(config,"\ncustom.external.change=yes\n",StandardOpenOption.APPEND);byte[] before=Files.readAllBytes(config);
        assertThatThrownBy(()->wizard.save(form)).hasMessage("wizard.stale");assertThat(Files.readAllBytes(config)).isEqualTo(before);verifyNoInteractions(old);
        var fresh=wizard.edit("Original");doThrow(new IllegalStateException("busy")).when(old).close();
        assertThatThrownBy(()->wizard.save(fresh)).isInstanceOf(IllegalStateException.class);assertThat(Files.readAllBytes(config)).isEqualTo(before);
        reset(old);
    }
    @Test void missingOrFakeCtbNeverCreatesDatabaseOrConfig()throws Exception {
        var form=new TenantWizardForm();form.path=directory.resolve("missing.ctb").toString();
        assertThatThrownBy(()->wizard.save(form)).isInstanceOf(java.io.IOException.class);assertThat(Files.exists(Path.of(form.path))).isFalse();
        Path fake=Files.writeString(directory.resolve("fake.ctb"),"not a database");form.path=fake.toString();
        assertThatThrownBy(()->wizard.save(form)).hasMessage("wizard.invalidCtb");assertThat(Files.exists(directory.resolve("allTenants"))).isFalse();
    }
    @Test void sameDatabaseWithDifferentNamesRemainsSupported()throws Exception {
        Path file=ctb();var one=new TenantWizardForm();one.path=file.toString();one.name="One";wizard.save(one);
        var two=new TenantWizardForm();two.path=file.toString();two.name="Two";two.writable=true;wizard.save(two);
        assertThat(routing.getResolvedDataSources()).hasSize(2);var duplicate=new TenantWizardForm();duplicate.path=file.toString();duplicate.name="One";
        assertThatThrownBy(()->wizard.save(duplicate)).hasMessage("wizard.duplicateName");
    }
    @Test void rejectsUnsupportedDriverAndInvalidLimitsAndUrl()throws Exception {
        var f=new TenantWizardForm();f.type="evil";assertThatThrownBy(()->wizard.save(f)).hasMessage("wizard.invalidConfig");
        f.type="postgresql";f.name="Remote";f.url="jdbc:sqlite:other.ctb";assertThatThrownBy(()->wizard.save(f)).hasMessage("wizard.invalidUrl");
        f.url="jdbc:postgresql://localhost/notes";f.maxEmbeddedFileSizeMB="21";assertThatThrownBy(()->wizard.save(f)).hasMessage("wizard.invalidLimit");
        f.maxEmbeddedFileSizeMB="9.5";assertThatThrownBy(()->wizard.save(f)).hasMessage("wizard.invalidLimit");
    }
}

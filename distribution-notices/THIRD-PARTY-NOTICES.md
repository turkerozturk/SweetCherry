# SweetCherry 1.0.0 — third-party notices

SweetCherry is distributed under GNU GPL v3. See the root `LICENSE` and [application source at v1.0.0](https://github.com/turkerozturk/SweetCherry/tree/v1.0.0). Application binaries come from commit `8f88bc943d3b1b772184dfd4d3936c5424e5fbce`. The `noticesCommit` in `windows-bundle.json` identifies the later documentation-only packaging revision; it does not change the application build commit.

Third-party components retain their own licenses. This inventory records the actual Windows bundle, original license files, source copyright notices and upstream references. It is an attribution record, not a legal certification. License alternatives in a POM are reproduced as declared, not a requirement to combine mutually alternative licenses.

## License files and source availability

`licenses/embedded-notices/` preserves standalone license/NOTICE files found in 121 of the 159 bundled JARs. `licenses/source-notices/` contains available notices and copyright headers extracted from corresponding source archives for JARs without standalone notices. `licenses/upstream-reference/` supplies additional license texts and upstream references. Whitespace and line endings of the extracted text copies are normalized for Git; original license files also remain inside the JARs. `JAR-INVENTORY.json` records artifact hashes, Maven coordinates, license metadata and corresponding-source archive URLs. Maven source links were checked when preparing this inventory; availability may change.

The application and its dependency JARs are not modified by this repack. Dependency JARs are stored in `BOOT-INF/lib/` and can be replaced/repacked with standard ZIP/JAR tools; compatibility of replacements is the user's responsibility. No restriction is imposed on modification or reverse engineering permitted by the component licenses. Source URLs in the inventory complement, rather than replace, the included license notices.

## Logo

The square SweetCherry logo combines Microsoft Fluent Emoji Farmer, Cherries and Seedling artwork. These assets are MIT licensed, copyright Microsoft Corporation. The original MIT notice is included. Composition does not imply endorsement. Exact artwork links and credits are in `licenses/LOGO-SOURCES.md`.

## Moon phase images

Moon-phase GIFs were obtained from the U.S. Naval Observatory [moon phases page](https://aa.usno.navy.mil/faq/moon_phases). USNO's site policy permits copying/distribution of its information unless otherwise stated and requests acknowledgement. Credit: U.S. Naval Observatory. See `licenses/MOON-SOURCES.md`. This credit concerns phase GIFs, not the separately credited Antonio Cidadao photograph montage on that page.

## CherryTree icons

CherryTree icon files were taken from CherryTree sources and converted/resized for SweetCherry. CherryTree is GPL v3; individual SVG metadata can instead specify GPL v2 or public-domain/CC0 artwork. Those individual declarations are preserved in `licenses/CHERRYTREE-SVG-METADATA.txt`; do not assume every icon has a single uniform license. Original SVG files and converted PNG files are available in the [SweetCherry v1.0.0 source tree](https://github.com/turkerozturk/SweetCherry/tree/v1.0.0/src/main/resources/static/img). Upstream: https://github.com/giuspen/cherrytree . Some SVG metadata identifies Openclipart/DooFi artwork. Retain that metadata when redistributing those files.

## Browser assets and fonts

- D3 7.9.0: ISC license; included upstream license text controls.
- Markmap: MIT. Bundled version could not be positively determined; no exact version is asserted. Its bundle includes d3-flextree 2.1.2. The current upstream d3-flextree license reference is WTFPL v2, not MIT; `d3-flextree-LICENSE-current.txt` is labelled as a current reference, not verified against the bundled release tag.
- Mermaid local bundle identifies version 12.0.0, MIT. Its bundled copyright/license block is preserved verbatim in `MERMAID-BUNDLED-NOTICES.txt`, including DOMPurify (Apache-2.0/MPL-2.0), Lodash and Cytoscape credits. Consult these component notices as well as the Mermaid license.
- Local Bootstrap CSS 5.3.3 and Bootstrap WebJar 5.3.8: Bootstrap MIT license. The WebJar POM's wrapper metadata is separate from the actual Bootstrap code license.
- highlight.js: its included license is preserved in `highlightjs-LICENSE.txt`.
- Font Awesome: icon artwork CC BY 4.0, fonts SIL OFL, code MIT as specified by its bundled LICENSE.
- Liberation fonts: their bundled license/copyright notice is preserved from `openpdf-fonts-extra`.
- Browser-split and class-list MIT notices additionally reconstruct the standard MIT terms using author/copyright information in the bundled package metadata. They are not represented as verbatim copies of an unavailable upstream LICENSE file. Original package metadata is retained alongside them.

## Bundled Java runtime and packaging tools

The runtime retains its own `runtime/NOTICE` and complete `runtime/legal/` directory. It identifies Eclipse Adoptium Temurin 17.0.20.1+1, JRE, Windows x64, principally GPL v2 with the Classpath Exception and component-specific notices. Its `runtime/release` records source revision `79597447bd94` in https://github.com/adoptium/jdk17u and build source revision `e6ba7dec3d07654074559310376a3ae89da5f4ac` in https://github.com/adoptium/temurin-build . Use those exact revisions for corresponding-source provenance.

Launch4j 3.50 creates the Windows launcher; its tool and generated wrapper have distinct licensing terms. The upstream current license reference included here is not asserted to be a byte-identical notice from the 3.50 tag. Consult https://launch4j.sourceforge.net/ and the upstream source. Inno Setup creates the installer; see https://jrsoftware.org/isinfo.php and https://jrsoftware.org/files/is/license.txt . Compiler tooling is not an application dependency.

## Bundled JAR inventory

Names below are declarations resolved from artifact/parent POM metadata, except where the inventory records embedded-text evidence. Original notices and full license texts take precedence over this summary.

| JAR | Declared license(s) | Corresponding source |
| --- | --- | --- |
| logback-classic-1.5.34.jar | EPL-2.0 / LGPL-2.1-only | [sources](https://repo.maven.apache.org/maven2/ch/qos/logback/logback-classic/1.5.34/logback-classic-1.5.34-sources.jar) |
| logback-core-1.5.34.jar | EPL-2.0 / LGPL-2.1-only | [sources](https://repo.maven.apache.org/maven2/ch/qos/logback/logback-core/1.5.34/logback-core-1.5.34-sources.jar) |
| log4j-to-slf4j-2.24.3.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/logging/log4j/log4j-to-slf4j/2.24.3/log4j-to-slf4j-2.24.3-sources.jar) |
| jul-to-slf4j-2.0.18.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/slf4j/jul-to-slf4j/2.0.18/jul-to-slf4j-2.0.18-sources.jar) |
| jakarta.annotation-api-2.1.1.jar | EPL 2.0 / GPL2 w/ CPE | [sources](https://repo.maven.apache.org/maven2/jakarta/annotation/jakarta.annotation-api/2.1.1/jakarta.annotation-api-2.1.1-sources.jar) |
| snakeyaml-2.4.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/yaml/snakeyaml/2.4/snakeyaml-2.4-sources.jar) |
| HikariCP-6.3.3.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/zaxxer/HikariCP/6.3.3/HikariCP-6.3.3-sources.jar) |
| spring-jdbc-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-jdbc/6.2.19/spring-jdbc-6.2.19-sources.jar) |
| hibernate-core-6.6.53.Final.jar | GNU Library General Public License v2.1 or later | [sources](https://repo.maven.apache.org/maven2/org/hibernate/orm/hibernate-core/6.6.53.Final/hibernate-core-6.6.53.Final-sources.jar) |
| jakarta.persistence-api-3.1.0.jar | Eclipse Public License v. 2.0 / Eclipse Distribution License v. 1.0 | [sources](https://repo.maven.apache.org/maven2/jakarta/persistence/jakarta.persistence-api/3.1.0/jakarta.persistence-api-3.1.0-sources.jar) |
| jakarta.transaction-api-2.0.1.jar | EPL 2.0 / GPL2 w/ CPE | [sources](https://repo.maven.apache.org/maven2/jakarta/transaction/jakarta.transaction-api/2.0.1/jakarta.transaction-api-2.0.1-sources.jar) |
| hibernate-commons-annotations-7.0.3.Final.jar | Apache License Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/hibernate/common/hibernate-commons-annotations/7.0.3.Final/hibernate-commons-annotations-7.0.3.Final-sources.jar) |
| jandex-3.2.0.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/io/smallrye/jandex/3.2.0/jandex-3.2.0-sources.jar) |
| classmate-1.7.3.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/fasterxml/classmate/1.7.3/classmate-1.7.3-sources.jar) |
| byte-buddy-1.17.8.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/net/bytebuddy/byte-buddy/1.17.8/byte-buddy-1.17.8-sources.jar) |
| jaxb-runtime-4.0.9.jar | Eclipse Distribution License - v 1.0 | [sources](https://repo.maven.apache.org/maven2/org/glassfish/jaxb/jaxb-runtime/4.0.9/jaxb-runtime-4.0.9-sources.jar) |
| jaxb-core-4.0.9.jar | Eclipse Distribution License - v 1.0 | [sources](https://repo.maven.apache.org/maven2/org/glassfish/jaxb/jaxb-core/4.0.9/jaxb-core-4.0.9-sources.jar) |
| txw2-4.0.9.jar | Eclipse Distribution License - v 1.0 | [sources](https://repo.maven.apache.org/maven2/org/glassfish/jaxb/txw2/4.0.9/txw2-4.0.9-sources.jar) |
| istack-commons-runtime-4.1.2.jar | Eclipse Distribution License - v 1.0 | [sources](https://repo.maven.apache.org/maven2/com/sun/istack/istack-commons-runtime/4.1.2/istack-commons-runtime-4.1.2-sources.jar) |
| jakarta.inject-api-2.0.1.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1-sources.jar) |
| antlr4-runtime-4.13.2.jar | BSD-3-Clause | [sources](https://repo.maven.apache.org/maven2/org/antlr/antlr4-runtime/4.13.2/antlr4-runtime-4.13.2-sources.jar) |
| spring-data-jpa-3.5.13.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/data/spring-data-jpa/3.5.13/spring-data-jpa-3.5.13-sources.jar) |
| spring-data-commons-3.5.13.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/data/spring-data-commons/3.5.13/spring-data-commons-3.5.13-sources.jar) |
| spring-orm-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-orm/6.2.19/spring-orm-6.2.19-sources.jar) |
| spring-context-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-context/6.2.19/spring-context-6.2.19-sources.jar) |
| spring-tx-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-tx/6.2.19/spring-tx-6.2.19-sources.jar) |
| spring-beans-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-beans/6.2.19/spring-beans-6.2.19-sources.jar) |
| spring-aspects-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-aspects/6.2.19/spring-aspects-6.2.19-sources.jar) |
| aspectjweaver-1.9.25.1.jar | Eclipse Public License - v 2.0 | [sources](https://repo.maven.apache.org/maven2/org/aspectj/aspectjweaver/1.9.25.1/aspectjweaver-1.9.25.1-sources.jar) |
| jackson-datatype-jdk8-2.21.4.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jdk8/2.21.4/jackson-datatype-jdk8-2.21.4-sources.jar) |
| jackson-datatype-jsr310-2.21.4.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.21.4/jackson-datatype-jsr310-2.21.4-sources.jar) |
| jackson-module-parameter-names-2.21.4.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/fasterxml/jackson/module/jackson-module-parameter-names/2.21.4/jackson-module-parameter-names-2.21.4-sources.jar) |
| tomcat-embed-core-10.1.55.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/tomcat/embed/tomcat-embed-core/10.1.55/tomcat-embed-core-10.1.55-sources.jar) |
| tomcat-embed-websocket-10.1.55.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/tomcat/embed/tomcat-embed-websocket/10.1.55/tomcat-embed-websocket-10.1.55-sources.jar) |
| spring-web-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-web/6.2.19/spring-web-6.2.19-sources.jar) |
| spring-webmvc-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-webmvc/6.2.19/spring-webmvc-6.2.19-sources.jar) |
| spring-expression-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-expression/6.2.19/spring-expression-6.2.19-sources.jar) |
| hibernate-community-dialects-6.6.53.Final.jar | GNU Library General Public License v2.1 or later | [sources](https://repo.maven.apache.org/maven2/org/hibernate/orm/hibernate-community-dialects/6.6.53.Final/hibernate-community-dialects-6.6.53.Final-sources.jar) |
| jboss-logging-3.6.3.Final.jar | Apache License 2.0 | [sources](https://repo.maven.apache.org/maven2/org/jboss/logging/jboss-logging/3.6.3.Final/jboss-logging-3.6.3.Final-sources.jar) |
| spring-context-support-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-context-support/6.2.19/spring-context-support-6.2.19-sources.jar) |
| thymeleaf-spring6-3.1.5.RELEASE.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/thymeleaf/thymeleaf-spring6/3.1.5.RELEASE/thymeleaf-spring6-3.1.5.RELEASE-sources.jar) |
| thymeleaf-layout-dialect-3.3.0.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/nz/net/ultraq/thymeleaf/thymeleaf-layout-dialect/3.3.0/thymeleaf-layout-dialect-3.3.0-sources.jar) |
| groovy-4.0.32.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/groovy/groovy/4.0.32/groovy-4.0.32-sources.jar) |
| groovy-extensions-2.1.0.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/nz/net/ultraq/groovy/groovy-extensions/2.1.0/groovy-extensions-2.1.0-sources.jar) |
| thymeleaf-expression-processor-3.2.0.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/nz/net/ultraq/thymeleaf/thymeleaf-expression-processor/3.2.0/thymeleaf-expression-processor-3.2.0-sources.jar) |
| thymeleaf-3.1.5.RELEASE.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/thymeleaf/thymeleaf/3.1.5.RELEASE/thymeleaf-3.1.5.RELEASE-sources.jar) |
| ognl-3.3.4.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/ognl/ognl/3.3.4/ognl-3.3.4-sources.jar) |
| javassist-3.29.0-GA.jar | MPL 1.1 / LGPL 2.1 / Apache License 2.0 | [sources](https://repo.maven.apache.org/maven2/org/javassist/javassist/3.29.0-GA/javassist-3.29.0-GA-sources.jar) |
| attoparser-2.0.7.RELEASE.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/attoparser/attoparser/2.0.7.RELEASE/attoparser-2.0.7.RELEASE-sources.jar) |
| unbescape-1.1.6.RELEASE.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/unbescape/unbescape/1.1.6.RELEASE/unbescape-1.1.6.RELEASE-sources.jar) |
| slf4j-api-2.0.18.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18-sources.jar) |
| htmx.org-4.0.0.jar | BSD-0-Clause | [sources](https://repo.maven.apache.org/maven2/org/webjars/npm/htmx.org/4.0.0/htmx.org-4.0.0-sources.jar) |
| hyperscript-2.0.2.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/webjars/npm/hyperscript/2.0.2/hyperscript-2.0.2-sources.jar) |
| browser-split-0.0.0.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/webjars/npm/browser-split/0.0.0/browser-split-0.0.0-sources.jar) |
| class-list-0.1.1.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/webjars/npm/class-list/0.1.1/class-list-0.1.1-sources.jar) |
| indexof-0.0.1.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/webjars/npm/indexof/0.0.1/indexof-0.0.1-sources.jar) |
| html-element-2.3.1.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/webjars/npm/html-element/2.3.1/html-element-2.3.1-sources.jar) |
| spring-boot-3.5.16.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot/3.5.16/spring-boot-3.5.16-sources.jar) |
| spring-boot-autoconfigure-3.5.16.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-autoconfigure/3.5.16/spring-boot-autoconfigure-3.5.16-sources.jar) |
| jakarta.xml.bind-api-4.0.5.jar | Eclipse Distribution License - v 1.0 | [sources](https://repo.maven.apache.org/maven2/jakarta/xml/bind/jakarta.xml.bind-api/4.0.5/jakarta.xml.bind-api-4.0.5-sources.jar) |
| jakarta.activation-api-2.1.4.jar | EDL 1.0 | [sources](https://repo.maven.apache.org/maven2/jakarta/activation/jakarta.activation-api/2.1.4/jakarta.activation-api-2.1.4-sources.jar) |
| spring-core-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-core/6.2.19/spring-core-6.2.19-sources.jar) |
| spring-jcl-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-jcl/6.2.19/spring-jcl-6.2.19-sources.jar) |
| tomcat-embed-el-10.1.55.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/tomcat/embed/tomcat-embed-el/10.1.55/tomcat-embed-el-10.1.55-sources.jar) |
| hibernate-validator-8.0.3.Final.jar | Apache License 2.0 | [sources](https://repo.maven.apache.org/maven2/org/hibernate/validator/hibernate-validator/8.0.3.Final/hibernate-validator-8.0.3.Final-sources.jar) |
| jakarta.validation-api-3.0.2.jar | Apache License 2.0 | [sources](https://repo.maven.apache.org/maven2/jakarta/validation/jakarta.validation-api/3.0.2/jakarta.validation-api-3.0.2-sources.jar) |
| sqlite-jdbc-3.53.4.0.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0-sources.jar) |
| spring-boot-actuator-autoconfigure-3.5.16.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-actuator-autoconfigure/3.5.16/spring-boot-actuator-autoconfigure-3.5.16-sources.jar) |
| spring-boot-actuator-3.5.16.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-actuator/3.5.16/spring-boot-actuator-3.5.16-sources.jar) |
| micrometer-observation-1.15.12.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/io/micrometer/micrometer-observation/1.15.12/micrometer-observation-1.15.12-sources.jar) |
| micrometer-commons-1.15.12.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/io/micrometer/micrometer-commons/1.15.12/micrometer-commons-1.15.12-sources.jar) |
| micrometer-jakarta9-1.15.12.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/io/micrometer/micrometer-jakarta9/1.15.12/micrometer-jakarta9-1.15.12-sources.jar) |
| micrometer-core-1.15.12.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/io/micrometer/micrometer-core/1.15.12/micrometer-core-1.15.12-sources.jar) |
| HdrHistogram-2.2.2.jar | Public Domain, per Creative Commons CC0 / BSD-2-Clause | [sources](https://repo.maven.apache.org/maven2/org/hdrhistogram/HdrHistogram/2.2.2/HdrHistogram-2.2.2-sources.jar) |
| LatencyUtils-2.0.3.jar | Public Domain, per Creative Commons CC0 | [sources](https://repo.maven.apache.org/maven2/org/latencyutils/LatencyUtils/2.0.3/LatencyUtils-2.0.3-sources.jar) |
| mysql-connector-j-9.0.0.jar | The GNU General Public License, v2 with Universal FOSS Exception, v1.0 | [sources](https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.0.0/mysql-connector-j-9.0.0-sources.jar) |
| postgresql-42.7.12.jar | BSD-2-Clause | [sources](https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.12/postgresql-42.7.12-sources.jar) |
| pdfbox-3.0.8.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/pdfbox/pdfbox/3.0.8/pdfbox-3.0.8-sources.jar) |
| pdfbox-io-3.0.8.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/pdfbox/pdfbox-io/3.0.8/pdfbox-io-3.0.8-sources.jar) |
| fontbox-3.0.8.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/pdfbox/fontbox/3.0.8/fontbox-3.0.8-sources.jar) |
| commons-logging-1.4.0.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/commons-logging/commons-logging/1.4.0/commons-logging-1.4.0-sources.jar) |
| openpdf-2.0.5.jar | GNU Lesser General Public License (LGPL), Version 2.1 / Mozilla Public License Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/github/librepdf/openpdf/2.0.5/openpdf-2.0.5-sources.jar) |
| openpdf-fonts-extra-2.0.5.jar | GNU Lesser General Public License (LGPL), Version 2.1 / Mozilla Public License Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/github/librepdf/openpdf-fonts-extra/2.0.5/openpdf-fonts-extra-2.0.5-sources.jar) |
| flying-saucer-pdf-9.13.3.jar | GNU Lesser General Public License (LGPL), version 2.1 or later | [sources](https://repo.maven.apache.org/maven2/org/xhtmlrenderer/flying-saucer-pdf/9.13.3/flying-saucer-pdf-9.13.3-sources.jar) |
| flying-saucer-core-9.13.3.jar | GNU Lesser General Public License (LGPL), version 2.1 or later | [sources](https://repo.maven.apache.org/maven2/org/xhtmlrenderer/flying-saucer-core/9.13.3/flying-saucer-core-9.13.3-sources.jar) |
| batik-codec-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-codec/1.19/batik-codec-1.19-sources.jar) |
| batik-awt-util-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-awt-util/1.19/batik-awt-util-1.19-sources.jar) |
| xmlgraphics-commons-2.11.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/xmlgraphics-commons/2.11/xmlgraphics-commons-2.11-sources.jar) |
| batik-bridge-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-bridge/1.19/batik-bridge-1.19-sources.jar) |
| batik-anim-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-anim/1.19/batik-anim-1.19-sources.jar) |
| batik-ext-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-ext/1.19/batik-ext-1.19-sources.jar) |
| batik-css-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-css/1.19/batik-css-1.19-sources.jar) |
| batik-dom-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-dom/1.19/batik-dom-1.19-sources.jar) |
| xml-apis-1.4.01.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/xml-apis/xml-apis/1.4.01/xml-apis-1.4.01-sources.jar) |
| batik-gvt-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-gvt/1.19/batik-gvt-1.19-sources.jar) |
| batik-parser-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-parser/1.19/batik-parser-1.19-sources.jar) |
| batik-script-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-script/1.19/batik-script-1.19-sources.jar) |
| batik-svg-dom-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-svg-dom/1.19/batik-svg-dom-1.19-sources.jar) |
| batik-xml-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-xml/1.19/batik-xml-1.19-sources.jar) |
| xml-apis-ext-1.3.04.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/xml-apis/xml-apis-ext/1.3.04/xml-apis-ext-1.3.04-sources.jar) |
| batik-shared-resources-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-shared-resources/1.19/batik-shared-resources-1.19-sources.jar) |
| batik-transcoder-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-transcoder/1.19/batik-transcoder-1.19-sources.jar) |
| batik-svggen-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-svggen/1.19/batik-svggen-1.19-sources.jar) |
| batik-util-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-util/1.19/batik-util-1.19-sources.jar) |
| batik-constants-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-constants/1.19/batik-constants-1.19-sources.jar) |
| batik-i18n-1.19.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlgraphics/batik-i18n/1.19/batik-i18n-1.19-sources.jar) |
| jsoup-1.23.2.jar | The MIT License | [sources](https://repo.maven.apache.org/maven2/org/jsoup/jsoup/1.23.2/jsoup-1.23.2-sources.jar) |
| commons-io-2.21.0.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/commons-io/commons-io/2.21.0/commons-io-2.21.0-sources.jar) |
| commons-lang3-3.21.0.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/commons/commons-lang3/3.21.0/commons-lang3-3.21.0-sources.jar) |
| poi-5.5.1.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/poi/poi/5.5.1/poi-5.5.1-sources.jar) |
| commons-codec-1.20.0.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/commons-codec/commons-codec/1.20.0/commons-codec-1.20.0-sources.jar) |
| commons-collections4-4.5.0.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/commons/commons-collections4/4.5.0/commons-collections4-4.5.0-sources.jar) |
| commons-math3-3.6.1.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1-sources.jar) |
| SparseBitSet-1.3.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/zaxxer/SparseBitSet/1.3/SparseBitSet-1.3-sources.jar) |
| log4j-api-2.24.3.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/logging/log4j/log4j-api/2.24.3/log4j-api-2.24.3-sources.jar) |
| poi-ooxml-5.5.1.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/poi/poi-ooxml/5.5.1/poi-ooxml-5.5.1-sources.jar) |
| poi-ooxml-lite-5.5.1.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/poi/poi-ooxml-lite/5.5.1/poi-ooxml-lite-5.5.1-sources.jar) |
| xmlbeans-5.3.0.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/xmlbeans/xmlbeans/5.3.0/xmlbeans-5.3.0-sources.jar) |
| commons-compress-1.28.0.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/1.28.0/commons-compress-1.28.0-sources.jar) |
| curvesapi-1.08.jar | BSD License | [sources](https://repo.maven.apache.org/maven2/com/github/virtuald/curvesapi/1.08/curvesapi-1.08-sources.jar) |
| webjars-locator-0.52.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/webjars/webjars-locator/0.52/webjars-locator-0.52-sources.jar) |
| webjars-locator-core-0.59.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/webjars/webjars-locator-core/0.59/webjars-locator-core-0.59-sources.jar) |
| classgraph-4.8.173.jar | The MIT License (MIT) | [sources](https://repo.maven.apache.org/maven2/io/github/classgraph/classgraph/4.8.173/classgraph-4.8.173-sources.jar) |
| jackson-core-2.21.4.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/fasterxml/jackson/core/jackson-core/2.21.4/jackson-core-2.21.4-sources.jar) |
| jackson-databind-2.21.4.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.21.4/jackson-databind-2.21.4-sources.jar) |
| jackson-annotations-2.21.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.21/jackson-annotations-2.21-sources.jar) |
| commons-text-1.11.0.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/apache/commons/commons-text/1.11.0/commons-text-1.11.0-sources.jar) |
| compiler-0.9.11.jar | Apache License 2.0 | [sources](https://repo.maven.apache.org/maven2/com/github/spullara/mustache/java/compiler/0.9.11/compiler-0.9.11-sources.jar) |
| bootstrap-5.3.8.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/webjars/bootstrap/5.3.8/bootstrap-5.3.8-sources.jar) |
| bootstrap-select-1.13.18.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/webjars/bootstrap-select/1.13.18/bootstrap-select-1.13.18-sources.jar) |
| popper.js-2.11.7.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/webjars/popper.js/2.11.7/popper.js-2.11.7-sources.jar) |
| jquery-3.7.1.jar | MIT License | [sources](https://repo.maven.apache.org/maven2/org/webjars/jquery/3.7.1/jquery-3.7.1-sources.jar) |
| jquery-ui-1.14.2+1.jar | MIT License | [sources](https://repo.maven.apache.org/maven2/org/webjars/jquery-ui/1.14.2+1/jquery-ui-1.14.2+1-sources.jar) |
| font-awesome-7.3.0.jar | (CC-BY-4.0 AND OFL-1.1 AND MIT) | [sources](https://repo.maven.apache.org/maven2/org/webjars/font-awesome/7.3.0/font-awesome-7.3.0-sources.jar) |
| jakarta.mail-2.0.5.jar | EPL 2.0 / GPL2 w/ CPE / EDL 1.0 | [sources](https://repo.maven.apache.org/maven2/jakarta/mail/jakarta.mail-api/2.1.5/jakarta.mail-api-2.1.5-sources.jar) |
| angus-activation-2.0.3.jar | EDL 1.0 | [sources](https://repo.maven.apache.org/maven2/org/eclipse/angus/angus-activation/2.0.3/angus-activation-2.0.3-sources.jar) |
| spring-aop-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-aop/6.2.19/spring-aop-6.2.19-sources.jar) |
| spring-security-config-6.5.11.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/security/spring-security-config/6.5.11/spring-security-config-6.5.11-sources.jar) |
| spring-security-web-6.5.11.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/security/spring-security-web/6.5.11/spring-security-web-6.5.11-sources.jar) |
| spring-messaging-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-messaging/6.2.19/spring-messaging-6.2.19-sources.jar) |
| spring-websocket-6.2.19.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/spring-websocket/6.2.19/spring-websocket-6.2.19-sources.jar) |
| thymeleaf-extras-springsecurity6-3.1.1.RELEASE.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/thymeleaf/extras/thymeleaf-extras-springsecurity6/3.1.1.RELEASE/thymeleaf-extras-springsecurity6-3.1.1.RELEASE-sources.jar) |
| spring-security-core-6.5.11.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/security/spring-security-core/6.5.11/spring-security-core-6.5.11-sources.jar) |
| spring-security-crypto-6.5.11.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/security/spring-security-crypto/6.5.11/spring-security-crypto-6.5.11-sources.jar) |
| commons-suncalc-3.11.jar | Apache License Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/shredzone/commons/commons-suncalc/3.11/commons-suncalc-3.11-sources.jar) |
| time4j-base-5.9.4.jar | GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1, February 1999 | [sources](https://repo.maven.apache.org/maven2/net/time4j/time4j-base/5.9.4/time4j-base-5.9.4-sources.jar) |
| time4j-sqlxml-5.9.4.jar | GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1, February 1999 | [sources](https://repo.maven.apache.org/maven2/net/time4j/time4j-sqlxml/5.9.4/time4j-sqlxml-5.9.4-sources.jar) |
| time4j-tzdata-5.0-2026b.jar | GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1, February 1999 | [sources](https://repo.maven.apache.org/maven2/net/time4j/time4j-tzdata/5.0-2026b/time4j-tzdata-5.0-2026b-sources.jar) |
| springdoc-openapi-starter-webmvc-ui-2.8.13.jar | The Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/2.8.13/springdoc-openapi-starter-webmvc-ui-2.8.13-sources.jar) |
| springdoc-openapi-starter-webmvc-api-2.8.13.jar | The Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-api/2.8.13/springdoc-openapi-starter-webmvc-api-2.8.13-sources.jar) |
| springdoc-openapi-starter-common-2.8.13.jar | The Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springdoc/springdoc-openapi-starter-common/2.8.13/springdoc-openapi-starter-common-2.8.13-sources.jar) |
| swagger-core-jakarta-2.2.36.jar | Apache License 2.0 | [sources](https://repo.maven.apache.org/maven2/io/swagger/core/v3/swagger-core-jakarta/2.2.36/swagger-core-jakarta-2.2.36-sources.jar) |
| swagger-annotations-jakarta-2.2.36.jar | Apache License 2.0 | [sources](https://repo.maven.apache.org/maven2/io/swagger/core/v3/swagger-annotations-jakarta/2.2.36/swagger-annotations-jakarta-2.2.36-sources.jar) |
| swagger-models-jakarta-2.2.36.jar | Apache License 2.0 | [sources](https://repo.maven.apache.org/maven2/io/swagger/core/v3/swagger-models-jakarta/2.2.36/swagger-models-jakarta-2.2.36-sources.jar) |
| jackson-dataformat-yaml-2.21.4.jar | The Apache Software License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.21.4/jackson-dataformat-yaml-2.21.4-sources.jar) |
| swagger-ui-5.28.1.jar | Apache-2.0 | [sources](https://repo.maven.apache.org/maven2/org/webjars/swagger-ui/5.28.1/swagger-ui-5.28.1-sources.jar) |
| webjars-locator-lite-1.1.3.jar | MIT | [sources](https://repo.maven.apache.org/maven2/org/webjars/webjars-locator-lite/1.1.3/webjars-locator-lite-1.1.3-sources.jar) |
| jspecify-1.0.0.jar | The Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/jspecify/jspecify/1.0.0/jspecify-1.0.0-sources.jar) |
| spring-boot-jarmode-tools-3.5.16.jar | Apache License, Version 2.0 | [sources](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-jarmode-tools/3.5.16/spring-boot-jarmode-tools-3.5.16-sources.jar) |

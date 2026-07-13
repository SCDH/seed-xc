# From DOI to Record

## Java DOI libraries

- https://github.com/gbif/gbif-api/blob/dev/src/main/java/org/gbif/api/model/common/DOI.java
- https://github.org/gbif/gbif-doi
- https://github.com/globalbioticinteractions/doi4j

## CURL Experiments

Beispiel ProdatPhil

```shell
curl https://doi.org/10.17879/91968401496
<html><head><title>Handle Redirect</title></head>
<body><a href="https://datastore.uni-muenster.de/doi/10.17879/91968401496">https://datastore.uni-muenster.de/doi/10.17879/91968401496</a></body></html>
```

Using `action=showurls` query parameter as documented in DOI: https://www.doi.org/the-identifier/resources/factsheets/doi-resolution-documentation

```shell
$ curl https://doi.org/10.17879/91968401496?action=showurls
<locations>
<location mode="legacy" weight="1.0" href="https://datastore.uni-muenster.de/doi/10.17879/91968401496"/>
<location weight="0" http_role="conneg" href="https://data.crosscite.org/10.17879%2F91968401496" href_template="https://data.crosscite.org/{hdl}"/>
</locations>
```

Taking that link:

```shell
$ curl https://datastore.uni-muenster.de/doi/10.17879/91968401496
<!doctype html>
<html lang=en>
<title>Redirecting...</title>
<h1>Redirecting...</h1>
<p>You should be redirected automatically to the target URL: <a href="https://datastore.uni-muenster.de/records/wjkxc-ck174">https://datastore.uni-muenster.de/records/wjkxc-ck174</a>. If not, click the link.
```

HTTP headers

```shell
$ curl -H GET -I https://datastore.uni-muenster.de/doi/10.17879/91968401496
HTTP/2 302 
server: istio-envoy
date: Fri, 15 May 2026 08:15:58 GMT
content-type: text/html; charset=utf-8
content-length: 293
location: https://datastore.uni-muenster.de/records/wjkxc-ck174
x-ratelimit-limit: 10
x-ratelimit-remaining: 9
x-ratelimit-reset: 1778832959
retry-after: 0
permissions-policy: interest-cohort=()
x-frame-options: sameorigin
x-xss-protection: 1; mode=block
x-content-type-options: nosniff
content-security-policy: default-src 'self' data: 'unsafe-inline' blob: https://matomo.uni-muenster.de/ 'wasm-unsafe-eval' https://s3.uni-muenster.de
strict-transport-security: max-age=31556926; includeSubDomains
strict-transport-security: max-age=15768000
referrer-policy: strict-origin-when-cross-origin
set-cookie: session=3f4c9b183f4a7c6b_6a06d63e.baXW7GLr2OviY9iRclvZGluyfPI; Expires=Mon, 15 Jun 2026 08:15:58 GMT; Secure; HttpOnly; Path=/; SameSite=Lax
x-request-id: 70ce3570e948edf0a40b1bda49fdabbe
x-envoy-upstream-service-time: 314
```

The redirection target `https://datastore.uni-muenster.de/records/wjkxc-ck174` is human-agent related HTML stuff.

Is there a way to get to an API instead of HTML stuff?

Yes! The header contains it:

```shell
$ curl -X GET https://datastore.uni-muenster.de/records/wjkxc-ck174 -I
HTTP/2 200 
server: istio-envoy
date: Fri, 15 May 2026 08:53:54 GMT
content-type: text/html; charset=utf-8
content-length: 108816
link: <https://datastore.uni-muenster.de/api/records/wjkxc-ck174> ; rel="linkset" ; type="application/linkset+json"
x-ratelimit-limit: 10
x-ratelimit-remaining: 10
x-ratelimit-reset: 1778835236
retry-after: 1
permissions-policy: interest-cohort=()
x-frame-options: sameorigin
x-xss-protection: 1; mode=block
x-content-type-options: nosniff
content-security-policy: default-src 'self' data: 'unsafe-inline' blob: https://matomo.uni-muenster.de/ 'wasm-unsafe-eval' https://s3.uni-muenster.de
strict-transport-security: max-age=31556926; includeSubDomains
strict-transport-security: max-age=15768000
referrer-policy: strict-origin-when-cross-origin
set-cookie: session=998c326e6eb21396_6a06df22.7DBChSK1zjCZz5i44r-7B3-46Q8; Expires=Mon, 15 Jun 2026 08:53:54 GMT; Secure; HttpOnly; Path=/; SameSite=Lax
x-request-id: 16db34694795b3c625c1f527289450c6
x-envoy-upstream-service-time: 8672
```

The Linkset has everything we need:

```shell
$ curl -X GET https://datastore.uni-muenster.de/api/records/wjkxc-ck174
{"id": "wjkxc-ck174", "created": "2025-09-26T00:00:00+00:00", "updated": "2026-02-09T17:05:26.738578+00:00", "links": {"self": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174", "self_html": "https://datastore.uni-muenster.de/records/wjkxc-ck174", "preview_html": "https://datastore.uni-muenster.de/records/wjkxc-ck174?preview=1", "doi": "https://doi.org/10.17879/91968401496", "self_doi": "https://doi.org/10.17879/91968401496", "self_doi_html": "https://datastore.uni-muenster.de/doi/10.17879/91968401496", "reserve_doi": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/draft/pids/doi", "parent": "https://datastore.uni-muenster.de/api/records/vstfr-h2k88", "parent_html": "https://datastore.uni-muenster.de/records/vstfr-h2k88", "parent_doi": "https://doi.org/10.17879/vstfr-h2k88", "parent_doi_html": "https://datastore.uni-muenster.de/doi/10.17879/vstfr-h2k88", "self_iiif_manifest": "https://datastore.uni-muenster.de/api/iiif/record:wjkxc-ck174/manifest", "self_iiif_sequence": "https://datastore.uni-muenster.de/api/iiif/record:wjkxc-ck174/sequence/default", "files": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files", "media_files": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/media-files", "archive": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files-archive", "archive_media": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/media-files-archive", "latest": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/versions/latest", "latest_html": "https://datastore.uni-muenster.de/records/wjkxc-ck174/latest", "versions": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/versions", "draft": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/draft", "access_links": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access/links", "access_grants": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access/grants", "access_users": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access/users", "access_groups": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access/groups", "access_request": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access/request", "access": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access", "communities": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/communities", "communities-suggestions": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/communities-suggestions", "request_deletion": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/request-deletion", "file_modification": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/file-modification", "requests": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/requests"}, "revision_id": 7, "parent": {"id": "vstfr-h2k88", "access": {"owned_by": {"user": "10306"}, "settings": {"allow_user_requests": false, "allow_guest_requests": false, "accept_conditions_text": null, "secret_link_expiration": 0}}, "communities": {}, "pids": {"doi": {"identifier": "10.17879/vstfr-h2k88", "provider": "datacite", "client": "datacite"}}}, "versions": {"is_latest": true, "index": 1}, "is_published": true, "is_draft": false, "pids": {"doi": {"identifier": "10.17879/91968401496", "provider": "datacite", "client": "datacite"}, "oai": {"identifier": "oai:datastore-rdm.com:wjkxc-ck174", "provider": "oai"}}, "metadata": {"resource_type": {"id": "dataset", "title": {"ar": "\u0645\u062c\u0645\u0648\u0639\u0629 \u0628\u064a\u0627\u0646\u0627\u062a", "cs": "Datov\u00e1 sada", "de": "Datensatz", "en": "Dataset", "es": "Conjunto de datos", "sv": "Dataset"}}, "creators": [{"person_or_org": {"type": "personal", "name": "He\u00dfbr\u00fcggen-Walter, Stefan", "given_name": "Stefan", "family_name": "He\u00dfbr\u00fcggen-Walter"}, "affiliations": [{"name": "Universit\u00e4t M\u00fcnster, Zentrum f\u00fcr Wissenschaftstheorie (ZfW)"}, {"id": "00pd74e08", "name": "Universit\u00e4t M\u00fcnster", "identifiers": [{"identifier": "00pd74e08", "scheme": "ror"}]}]}, {"person_or_org": {"type": "personal", "name": "Frank, Ingo", "given_name": "Ingo", "family_name": "Frank"}, "affiliations": [{"name": "Universit\u00e4t M\u00fcnster, Universit\u00e4ts- und Landesbibliothek, ULB Stabsreferat R1 Wissenschaft & Innovation, ULB R1 Referat 1 Forschung und Entwicklung, ULB R1.2 Teilreferat Digital Humanities"}, {"id": "00pd74e08", "name": "Universit\u00e4t M\u00fcnster", "identifiers": [{"identifier": "00pd74e08", "scheme": "ror"}]}]}], "title": "PRODATPHIL -- Science and Logic v. 0.2", "additional_titles": [{"title": "Datensammlung zu Logik und Wissenschaftsphilosophie des 19. Jahrhunderts", "type": {"id": "subtitle", "title": {"cs": "Podtitul", "de": "Untertitel", "en": "Subtitle", "es": "Subt\u00edtulo", "sv": "Undertitel"}}}], "publisher": "University of M\u00fcnster", "publication_date": "2025-09-26", "subjects": [{"subject": "digital humanities"}, {"subject": "digital philosophy"}, {"subject": "data collection"}, {"subject": "logic"}, {"subject": "philosophy of science"}], "contributors": [{"person_or_org": {"type": "personal", "name": "He\u00dfbr\u00fcggen-Walter, Stefan", "given_name": "Stefan", "family_name": "He\u00dfbr\u00fcggen-Walter"}, "role": {"id": "researcher", "title": {"cs": "V\u00fdzkumn\u00edk", "de": "WissenschaftlerIn", "en": "Researcher", "es": "Investigador", "sv": "Forskare"}}, "affiliations": [{"name": "Universit\u00e4t M\u00fcnster, Zentrum f\u00fcr Wissenschaftstheorie (ZfW)"}, {"id": "00pd74e08", "name": "Universit\u00e4t M\u00fcnster", "identifiers": [{"identifier": "00pd74e08", "scheme": "ror"}]}]}, {"person_or_org": {"type": "personal", "name": "Frank, Ingo", "given_name": "Ingo", "family_name": "Frank"}, "role": {"id": "researcher", "title": {"cs": "V\u00fdzkumn\u00edk", "de": "WissenschaftlerIn", "en": "Researcher", "es": "Investigador", "sv": "Forskare"}}, "affiliations": [{"name": "Universit\u00e4t M\u00fcnster, Universit\u00e4ts- und Landesbibliothek, ULB Stabsreferat R1 Wissenschaft & Innovation, ULB R1 Referat 1 Forschung und Entwicklung, ULB R1.2 Teilreferat Digital Humanities"}, {"id": "00pd74e08", "name": "Universit\u00e4t M\u00fcnster", "identifiers": [{"identifier": "00pd74e08", "scheme": "ror"}]}]}], "dates": [{"date": "2025", "type": {"id": "created", "title": {"cs": "Vytvo\u0159eno", "de": "Erstellt", "en": "Created", "es": "Creado", "sv": "Skapad"}}}], "languages": [{"id": "deu", "title": {"de": "Deutsch", "en": "German", "fr": "allemand"}}], "related_identifiers": [{"identifier": "10.17879/35948496290", "scheme": "doi", "relation_type": {"id": "continues", "title": {"cs": "Pokra\u010duje (\u010d\u00edm)", "de": "Setzt fort", "en": "Continues", "es": "Contin\u00faa", "sv": "Forts\u00e4tter"}}, "resource_type": {"id": "dataset", "title": {"ar": "\u0645\u062c\u0645\u0648\u0639\u0629 \u0628\u064a\u0627\u0646\u0627\u062a", "cs": "Datov\u00e1 sada", "de": "Datensatz", "en": "Dataset", "es": "Conjunto de datos", "sv": "Dataset"}}}], "formats": ["ZIP"], "rights": [{"id": "cc0-1.0", "title": {"en": "CC0 1.0"}, "description": {"en": "Public domain dedication that waives all copyright and related rights worldwide"}, "icon": "cc-cc0-icon", "props": {"url": "https://spdx.org/licenses/CC0-1.0", "scheme": "spdx"}}], "description": "This data set is an output of the DFG-funded project \"Prodatphil -- Science and Logic\" (project number 537184692), a cooperation of the Center of Philosophy of Science and the Service Center for Digital Humanities of the university of M\u00fcnster (principal investigator Stefan He\u00dfbr\u00fcggen-Walter). The project started in July 2024 and is currently funded until July 2027.\n\nAuthors in alphabetic order: Ingo Frank (metadata), Stefan He\u00dfbr\u00fcggen-Walter (conception, construction of the corpus)\n\nThis is an alpha version with no guarantees and subject to change.\n\nScope\nThe data set aims at researchers interested in the application of DH methods to philosophical texts. It is limited to texts in the public domain from the 19th century in English. Future versions will include German and French source texts as well.\n\nPlease note that these are historical sources which may contain racist tropes or other content that discriminates against groups of people. Data are made available for research purposes and are not to be understood as propaganda or an endorsement of discriminatory practices.\n\nContent\n\n\nThis corpus contains in sum 207 texts, 72 books and 135 articles. 16 texts were published anonymously. The remaining texts were written by 119 known authors and published between 1830 and 1956. Articles were published in 13 journals.\n\nThe English subcorpus contains in sum 164 texts, 42 books and 122 articles. 15 texts were published anonymously. The remaining texts were written by 97 known authors and published between 1839 and 1930. Articles were published in 10 journals.\n\nThe French subcorpus contains in sum 36 texts, 23 books and 13 articles. 1 text was published anonymously. The remaining texts were written by 20 known authors and published between 1830 and 1956. Articles were published in 3 journals.\n\nThe German subcorpus contains in sum 7 texts, 7 books and no articles. No texts were published anonymously. The texts were written by 6 known authors and published between 1868 and 1919. No articles were published in journals.\n\nMore details in the README of the data set and a notebook containing a corpus description.\n\n\nTechnical requirements\nThe data set comprises TEI files and metadata. All data can be accessed using standard software (text or XML editor, spreadsheet software, browser). No special tools are required.\n\nData provenance\nWe retrieved digital full texts from https://www.gutenberg.org and https://en.wikisource.org. Since the data are hosted in Germany, German copyright law applies. Therefore the date of death of the original author rather than the year of publication is the criterion for whether or not a text is in the public domain.\n\nData model\nThe data set contains two three CSV files containing metadata and 94 files containing TEI encoded text. The metadata format is not yet definitive and errors in the TEI encoding are possible. These known bugs will be corrected in the next update.\nSome texts were published as a series of installments. The original metadata have been preserved, information linking this bibliographical information to the files in the data set can be found in metadata_series.csv.\n\nData reuse\n\nData and metadata are in the public domain.\n\nAcknowledgments\n\nThis data set description was inspired by Middle, S. A documentation checklist for (Linked) humanities data. Int J Digit Humanities 5, 353\u2013371 (2023). https://doi.org/10.1007/s42803-023-00072-z", "funding": [{"funder": {"id": "018mejw64", "name": "Deutsche Forschungsgemeinschaft"}, "award": {"number": "537184692", "identifiers": [{"identifier": "https://gepris.dfg.de/gepris/projekt/537184692", "scheme": "url"}]}}]}, "custom_fields": {}, "access": {"record": "public", "files": "public", "embargo": {"active": false, "reason": null}, "status": "open"}, "files": {"enabled": true, "order": [], "count": 1, "total_bytes": 147748661, "entries": {"prodatphil_releases-main-0.2.zip": {"id": "c8934e9f-f16f-4d56-b3bb-c38d814e1b5e", "checksum": "md5:e2c00625c99c8fcd15593b9b5553cbda", "ext": "zip", "size": 147748661, "mimetype": "application/zip", "storage_class": "L", "key": "prodatphil_releases-main-0.2.zip", "metadata": {}, "access": {"hidden": false}, "links": {"self": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files/prodatphil_releases-main-0.2.zip", "content": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files/prodatphil_releases-main-0.2.zip/content"}}}}, "media_files": {"enabled": false, "order": [], "count": 0, "total_bytes": 0, "entries": {}}, "status": "published", "deletion_status": {"is_deleted": false, "status": "P"}, "stats": {"this_version": {"views": 50, "unique_views": 40, "downloads": 16, "unique_downloads": 16, "data_volume": 2363978576.0}, "all_versions": {"views": 50, "unique_views": 40, "downloads": 16, "unique_downloads": 16, "data_volume": 2363978576.0}}}
```

For reading:

```shell
$ curl -X GET https://datastore.uni-muenster.de/api/records/wjkxc-ck174 | jq
{
  "id": "wjkxc-ck174",
  "created": "2025-09-26T00:00:00+00:00",
  "updated": "2026-02-09T17:05:26.738578+00:00",
  "links": {
    "self": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174",
    "self_html": "https://datastore.uni-muenster.de/records/wjkxc-ck174",
    "preview_html": "https://datastore.uni-muenster.de/records/wjkxc-ck174?preview=1",
    "doi": "https://doi.org/10.17879/91968401496",
    "self_doi": "https://doi.org/10.17879/91968401496",
    "self_doi_html": "https://datastore.uni-muenster.de/doi/10.17879/91968401496",
    "reserve_doi": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/draft/pids/doi",
    "parent": "https://datastore.uni-muenster.de/api/records/vstfr-h2k88",
    "parent_html": "https://datastore.uni-muenster.de/records/vstfr-h2k88",
    "parent_doi": "https://doi.org/10.17879/vstfr-h2k88",
    "parent_doi_html": "https://datastore.uni-muenster.de/doi/10.17879/vstfr-h2k88",
    "self_iiif_manifest": "https://datastore.uni-muenster.de/api/iiif/record:wjkxc-ck174/manifest",
    "self_iiif_sequence": "https://datastore.uni-muenster.de/api/iiif/record:wjkxc-ck174/sequence/default",
    "files": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files",
    "media_files": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/media-files",
    "archive": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files-archive",
    "archive_media": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/media-files-archive",
    "latest": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/versions/latest",
    "latest_html": "https://datastore.uni-muenster.de/records/wjkxc-ck174/latest",
    "versions": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/versions",
    "draft": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/draft",
    "access_links": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access/links",
    "access_grants": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access/grants",
    "access_users": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access/users",
    "access_groups": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access/groups",
    "access_request": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access/request",
    "access": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/access",
    "communities": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/communities",
    "communities-suggestions": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/communities-suggestions",
    "request_deletion": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/request-deletion",
    "file_modification": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/file-modification",
    "requests": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/requests"
  },
  "revision_id": 7,
  "parent": {
    "id": "vstfr-h2k88",
    "access": {
      "owned_by": {
        "user": "10306"
      },
      "settings": {
        "allow_user_requests": false,
        "allow_guest_requests": false,
        "accept_conditions_text": null,
        "secret_link_expiration": 0
      }
    },
    "communities": {},
    "pids": {
      "doi": {
        "identifier": "10.17879/vstfr-h2k88",
        "provider": "datacite",
        "client": "datacite"
      }
    }
  },
  "versions": {
    "is_latest": true,
    "index": 1
  },
  "is_published": true,
  "is_draft": false,
  "pids": {
    "doi": {
      "identifier": "10.17879/91968401496",
      "provider": "datacite",
      "client": "datacite"
    },
    "oai": {
      "identifier": "oai:datastore-rdm.com:wjkxc-ck174",
      "provider": "oai"
    }
  },
  "metadata": {
    "resource_type": {
      "id": "dataset",
      "title": {
        "ar": "مجموعة بيانات",
        "cs": "Datová sada",
        "de": "Datensatz",
        "en": "Dataset",
        "es": "Conjunto de datos",
        "sv": "Dataset"
      }
    },
    "creators": [
      {
        "person_or_org": {
          "type": "personal",
          "name": "Heßbrüggen-Walter, Stefan",
          "given_name": "Stefan",
          "family_name": "Heßbrüggen-Walter"
        },
        "affiliations": [
          {
            "name": "Universität Münster, Zentrum für Wissenschaftstheorie (ZfW)"
          },
          {
            "id": "00pd74e08",
            "name": "Universität Münster",
            "identifiers": [
              {
                "identifier": "00pd74e08",
                "scheme": "ror"
              }
            ]
          }
        ]
      },
      {
        "person_or_org": {
          "type": "personal",
          "name": "Frank, Ingo",
          "given_name": "Ingo",
          "family_name": "Frank"
        },
        "affiliations": [
          {
            "name": "Universität Münster, Universitäts- und Landesbibliothek, ULB Stabsreferat R1 Wissenschaft & Innovation, ULB R1 Referat 1 Forschung und Entwicklung, ULB R1.2 Teilreferat Digital Humanities"
          },
          {
            "id": "00pd74e08",
            "name": "Universität Münster",
            "identifiers": [
              {
                "identifier": "00pd74e08",
                "scheme": "ror"
              }
            ]
          }
        ]
      }
    ],
    "title": "PRODATPHIL -- Science and Logic v. 0.2",
    "additional_titles": [
      {
        "title": "Datensammlung zu Logik und Wissenschaftsphilosophie des 19. Jahrhunderts",
        "type": {
          "id": "subtitle",
          "title": {
            "cs": "Podtitul",
            "de": "Untertitel",
            "en": "Subtitle",
            "es": "Subtítulo",
            "sv": "Undertitel"
          }
        }
      }
    ],
    "publisher": "University of Münster",
    "publication_date": "2025-09-26",
    "subjects": [
      {
        "subject": "digital humanities"
      },
      {
        "subject": "digital philosophy"
      },
      {
        "subject": "data collection"
      },
      {
        "subject": "logic"
      },
      {
        "subject": "philosophy of science"
      }
    ],
    "contributors": [
      {
        "person_or_org": {
          "type": "personal",
          "name": "Heßbrüggen-Walter, Stefan",
          "given_name": "Stefan",
          "family_name": "Heßbrüggen-Walter"
        },
        "role": {
          "id": "researcher",
          "title": {
            "cs": "Výzkumník",
            "de": "WissenschaftlerIn",
            "en": "Researcher",
            "es": "Investigador",
            "sv": "Forskare"
          }
        },
        "affiliations": [
          {
            "name": "Universität Münster, Zentrum für Wissenschaftstheorie (ZfW)"
          },
          {
            "id": "00pd74e08",
            "name": "Universität Münster",
            "identifiers": [
              {
                "identifier": "00pd74e08",
                "scheme": "ror"
              }
            ]
          }
        ]
      },
      {
        "person_or_org": {
          "type": "personal",
          "name": "Frank, Ingo",
          "given_name": "Ingo",
          "family_name": "Frank"
        },
        "role": {
          "id": "researcher",
          "title": {
            "cs": "Výzkumník",
            "de": "WissenschaftlerIn",
            "en": "Researcher",
            "es": "Investigador",
            "sv": "Forskare"
          }
        },
        "affiliations": [
          {
            "name": "Universität Münster, Universitäts- und Landesbibliothek, ULB Stabsreferat R1 Wissenschaft & Innovation, ULB R1 Referat 1 Forschung und Entwicklung, ULB R1.2 Teilreferat Digital Humanities"
          },
          {
            "id": "00pd74e08",
            "name": "Universität Münster",
            "identifiers": [
              {
                "identifier": "00pd74e08",
                "scheme": "ror"
              }
            ]
          }
        ]
      }
    ],
    "dates": [
      {
        "date": "2025",
        "type": {
          "id": "created",
          "title": {
            "cs": "Vytvořeno",
            "de": "Erstellt",
            "en": "Created",
            "es": "Creado",
            "sv": "Skapad"
          }
        }
      }
    ],
    "languages": [
      {
        "id": "deu",
        "title": {
          "de": "Deutsch",
          "en": "German",
          "fr": "allemand"
        }
      }
    ],
    "related_identifiers": [
      {
        "identifier": "10.17879/35948496290",
        "scheme": "doi",
        "relation_type": {
          "id": "continues",
          "title": {
            "cs": "Pokračuje (čím)",
            "de": "Setzt fort",
            "en": "Continues",
            "es": "Continúa",
            "sv": "Fortsätter"
          }
        },
        "resource_type": {
          "id": "dataset",
          "title": {
            "ar": "مجموعة بيانات",
            "cs": "Datová sada",
            "de": "Datensatz",
            "en": "Dataset",
            "es": "Conjunto de datos",
            "sv": "Dataset"
          }
        }
      }
    ],
    "formats": [
      "ZIP"
    ],
    "rights": [
      {
        "id": "cc0-1.0",
        "title": {
          "en": "CC0 1.0"
        },
        "description": {
          "en": "Public domain dedication that waives all copyright and related rights worldwide"
        },
        "icon": "cc-cc0-icon",
        "props": {
          "url": "https://spdx.org/licenses/CC0-1.0",
          "scheme": "spdx"
        }
      }
    ],
    "description": "This data set is an output of the DFG-funded project \"Prodatphil -- Science and Logic\" (project number 537184692), a cooperation of the Center of Philosophy of Science and the Service Center for Digital Humanities of the university of Münster (principal investigator Stefan Heßbrüggen-Walter). The project started in July 2024 and is currently funded until July 2027.\n\nAuthors in alphabetic order: Ingo Frank (metadata), Stefan Heßbrüggen-Walter (conception, construction of the corpus)\n\nThis is an alpha version with no guarantees and subject to change.\n\nScope\nThe data set aims at researchers interested in the application of DH methods to philosophical texts. It is limited to texts in the public domain from the 19th century in English. Future versions will include German and French source texts as well.\n\nPlease note that these are historical sources which may contain racist tropes or other content that discriminates against groups of people. Data are made available for research purposes and are not to be understood as propaganda or an endorsement of discriminatory practices.\n\nContent\n\n\nThis corpus contains in sum 207 texts, 72 books and 135 articles. 16 texts were published anonymously. The remaining texts were written by 119 known authors and published between 1830 and 1956. Articles were published in 13 journals.\n\nThe English subcorpus contains in sum 164 texts, 42 books and 122 articles. 15 texts were published anonymously. The remaining texts were written by 97 known authors and published between 1839 and 1930. Articles were published in 10 journals.\n\nThe French subcorpus contains in sum 36 texts, 23 books and 13 articles. 1 text was published anonymously. The remaining texts were written by 20 known authors and published between 1830 and 1956. Articles were published in 3 journals.\n\nThe German subcorpus contains in sum 7 texts, 7 books and no articles. No texts were published anonymously. The texts were written by 6 known authors and published between 1868 and 1919. No articles were published in journals.\n\nMore details in the README of the data set and a notebook containing a corpus description.\n\n\nTechnical requirements\nThe data set comprises TEI files and metadata. All data can be accessed using standard software (text or XML editor, spreadsheet software, browser). No special tools are required.\n\nData provenance\nWe retrieved digital full texts from https://www.gutenberg.org and https://en.wikisource.org. Since the data are hosted in Germany, German copyright law applies. Therefore the date of death of the original author rather than the year of publication is the criterion for whether or not a text is in the public domain.\n\nData model\nThe data set contains two three CSV files containing metadata and 94 files containing TEI encoded text. The metadata format is not yet definitive and errors in the TEI encoding are possible. These known bugs will be corrected in the next update.\nSome texts were published as a series of installments. The original metadata have been preserved, information linking this bibliographical information to the files in the data set can be found in metadata_series.csv.\n\nData reuse\n\nData and metadata are in the public domain.\n\nAcknowledgments\n\nThis data set description was inspired by Middle, S. A documentation checklist for (Linked) humanities data. Int J Digit Humanities 5, 353–371 (2023). https://doi.org/10.1007/s42803-023-00072-z",
    "funding": [
      {
        "funder": {
          "id": "018mejw64",
          "name": "Deutsche Forschungsgemeinschaft"
        },
        "award": {
          "number": "537184692",
          "identifiers": [
            {
              "identifier": "https://gepris.dfg.de/gepris/projekt/537184692",
              "scheme": "url"
            }
          ]
        }
      }
    ]
  },
  "custom_fields": {},
  "access": {
    "record": "public",
    "files": "public",
    "embargo": {
      "active": false,
      "reason": null
    },
    "status": "open"
  },
  "files": {
    "enabled": true,
    "order": [],
    "count": 1,
    "total_bytes": 147748661,
    "entries": {
      "prodatphil_releases-main-0.2.zip": {
        "id": "c8934e9f-f16f-4d56-b3bb-c38d814e1b5e",
        "checksum": "md5:e2c00625c99c8fcd15593b9b5553cbda",
        "ext": "zip",
        "size": 147748661,
        "mimetype": "application/zip",
        "storage_class": "L",
        "key": "prodatphil_releases-main-0.2.zip",
        "metadata": {},
        "access": {
          "hidden": false
        },
        "links": {
          "self": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files/prodatphil_releases-main-0.2.zip",
          "content": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files/prodatphil_releases-main-0.2.zip/content"
        }
      }
    }
  },
  "media_files": {
    "enabled": false,
    "order": [],
    "count": 0,
    "total_bytes": 0,
    "entries": {}
  },
  "status": "published",
  "deletion_status": {
    "is_deleted": false,
    "status": "P"
  },
  "stats": {
    "this_version": {
      "views": 50,
      "unique_views": 40,
      "downloads": 16,
      "unique_downloads": 16,
      "data_volume": 2363978576.0
    },
    "all_versions": {
      "views": 50,
      "unique_views": 40,
      "downloads": 16,
      "unique_downloads": 16,
      "data_volume": 2363978576.0
    }
  }
}
```

Obviously, application/linkset+json is specified by IETF: https://www.ietf.org/archive/id/draft-ietf-httpapi-linkset-03.html

OAI: There is an OAI ID in the above JSON:

```json
  "pids": {
    "doi": {
      "identifier": "10.17879/91968401496",
      "provider": "datacite",
      "client": "datacite"
    },
    "oai": {
      "identifier": "oai:datastore-rdm.com:wjkxc-ck174",
      "provider": "oai"
    }
  },
```

But it does not resolve. Is datastore configured correctly? `.com` looks bad!


What other formats are available?

```shell
$ curl -X GET https://datastore.uni-muenster.de/api/records/wjkxc-ck174 -H "Accept: application/linkset+xml"
{"status": 406, "message": "Invalid 'Accept' header. Expected one of: application/json, application/ld+json, application/vnd.inveniordm.v1.full+csv, application/vnd.inveniordm.v1.simple+csv, application/marcxml+xml, application/vnd.inveniordm.v1+json, application/vnd.citationstyles.csl+json, application/vnd.datacite.datacite+json, application/vnd.geo+json, application/vnd.datacite.datacite+xml, application/ld+json;profile=\"https://datapackage.org/profiles/2.0/datapackage.json\", application/x-dc+xml, text/x-bibliography, application/x-bibtex, application/dcat+xml, application/linkset+json"}
```

Andere Datenformate:

JSON-LD

```shell
{
  "@context": "http://schema.org",
  "@id": "https://doi.org/10.17879/91968401496",
  "@type": "https://schema.org/Dataset",
  "identifier": "https://doi.org/10.17879/91968401496",
  "name": "PRODATPHIL -- Science and Logic v. 0.2",
  "creator": [
    {
      "name": "Heßbrüggen-Walter, Stefan",
      "givenName": "Stefan",
      "familyName": "Heßbrüggen-Walter",
      "affiliation": [
        {
          "@type": "Organization",
          "name": "Universität Münster, Zentrum für Wissenschaftstheorie (ZfW)"
        },
        {
          "@type": "Organization",
          "name": "Universität Münster",
          "@id": "https://ror.org/00pd74e08"
        }
      ],
      "@type": "Person"
    },
    {
      "name": "Frank, Ingo",
      "givenName": "Ingo",
      "familyName": "Frank",
      "affiliation": [
        {
          "@type": "Organization",
          "name": "Universität Münster, Universitäts- und Landesbibliothek, ULB Stabsreferat R1 Wissenschaft & Innovation, ULB R1 Referat 1 Forschung und Entwicklung, ULB R1.2 Teilreferat Digital Humanities"
        },
        {
          "@type": "Organization",
          "name": "Universität Münster",
          "@id": "https://ror.org/00pd74e08"
        }
      ],
      "@type": "Person"
    }
  ],
  "author": [
    {
      "name": "Heßbrüggen-Walter, Stefan",
      "givenName": "Stefan",
      "familyName": "Heßbrüggen-Walter",
      "affiliation": [
        {
          "@type": "Organization",
          "name": "Universität Münster, Zentrum für Wissenschaftstheorie (ZfW)"
        },
        {
          "@type": "Organization",
          "name": "Universität Münster",
          "@id": "https://ror.org/00pd74e08"
        }
      ],
      "@type": "Person"
    },
    {
      "name": "Frank, Ingo",
      "givenName": "Ingo",
      "familyName": "Frank",
      "affiliation": [
        {
          "@type": "Organization",
          "name": "Universität Münster, Universitäts- und Landesbibliothek, ULB Stabsreferat R1 Wissenschaft & Innovation, ULB R1 Referat 1 Forschung und Entwicklung, ULB R1.2 Teilreferat Digital Humanities"
        },
        {
          "@type": "Organization",
          "name": "Universität Münster",
          "@id": "https://ror.org/00pd74e08"
        }
      ],
      "@type": "Person"
    }
  ],
  "editor": [
    {
      "name": "Heßbrüggen-Walter, Stefan",
      "givenName": "Stefan",
      "familyName": "Heßbrüggen-Walter",
      "affiliation": [
        {
          "@type": "Organization",
          "name": "Universität Münster, Zentrum für Wissenschaftstheorie (ZfW)"
        },
        {
          "@type": "Organization",
          "name": "Universität Münster",
          "@id": "https://ror.org/00pd74e08"
        }
      ],
      "@type": "Person"
    },
    {
      "name": "Frank, Ingo",
      "givenName": "Ingo",
      "familyName": "Frank",
      "affiliation": [
        {
          "@type": "Organization",
          "name": "Universität Münster, Universitäts- und Landesbibliothek, ULB Stabsreferat R1 Wissenschaft & Innovation, ULB R1 Referat 1 Forschung und Entwicklung, ULB R1.2 Teilreferat Digital Humanities"
        },
        {
          "@type": "Organization",
          "name": "Universität Münster",
          "@id": "https://ror.org/00pd74e08"
        }
      ],
      "@type": "Person"
    }
  ],
  "publisher": {
    "@type": "Organization",
    "name": "University of Münster"
  },
  "keywords": "digital humanities, digital philosophy, data collection, logic, philosophy of science",
  "dateCreated": "2025-09-26T00:00:00+00:00",
  "dateModified": "2026-02-09T17:05:26.738578+00:00",
  "datePublished": "2025-09-26",
  "temporal": [
    "2025"
  ],
  "inLanguage": {
    "alternateName": "deu",
    "@type": "Language",
    "name": "German"
  },
  "contentSize": "140.9 MB",
  "size": "140.9 MB",
  "encodingFormat": "ZIP",
  "license": "https://creativecommons.org/publicdomain/zero/1.0/legalcode",
  "description": "This data set is an output of the DFG-funded project \"Prodatphil -- Science and Logic\" (project number 537184692), a cooperation of the Center of Philosophy of Science and the Service Center for Digital Humanities of the university of Münster (principal investigator Stefan Heßbrüggen-Walter). The project started in July 2024 and is currently funded until July 2027.\n\nAuthors in alphabetic order: Ingo Frank (metadata), Stefan Heßbrüggen-Walter (conception, construction of the corpus)\n\nThis is an alpha version with no guarantees and subject to change.\n\nScope\nThe data set aims at researchers interested in the application of DH methods to philosophical texts. It is limited to texts in the public domain from the 19th century in English. Future versions will include German and French source texts as well.\n\nPlease note that these are historical sources which may contain racist tropes or other content that discriminates against groups of people. Data are made available for research purposes and are not to be understood as propaganda or an endorsement of discriminatory practices.\n\nContent\n\n\nThis corpus contains in sum 207 texts, 72 books and 135 articles. 16 texts were published anonymously. The remaining texts were written by 119 known authors and published between 1830 and 1956. Articles were published in 13 journals.\n\nThe English subcorpus contains in sum 164 texts, 42 books and 122 articles. 15 texts were published anonymously. The remaining texts were written by 97 known authors and published between 1839 and 1930. Articles were published in 10 journals.\n\nThe French subcorpus contains in sum 36 texts, 23 books and 13 articles. 1 text was published anonymously. The remaining texts were written by 20 known authors and published between 1830 and 1956. Articles were published in 3 journals.\n\nThe German subcorpus contains in sum 7 texts, 7 books and no articles. No texts were published anonymously. The texts were written by 6 known authors and published between 1868 and 1919. No articles were published in journals.\n\nMore details in the README of the data set and a notebook containing a corpus description.\n\n\nTechnical requirements\nThe data set comprises TEI files and metadata. All data can be accessed using standard software (text or XML editor, spreadsheet software, browser). No special tools are required.\n\nData provenance\nWe retrieved digital full texts from https://www.gutenberg.org and https://en.wikisource.org. Since the data are hosted in Germany, German copyright law applies. Therefore the date of death of the original author rather than the year of publication is the criterion for whether or not a text is in the public domain.\n\nData model\nThe data set contains two three CSV files containing metadata and 94 files containing TEI encoded text. The metadata format is not yet definitive and errors in the TEI encoding are possible. These known bugs will be corrected in the next update.\nSome texts were published as a series of installments. The original metadata have been preserved, information linking this bibliographical information to the files in the data set can be found in metadata_series.csv.\n\nData reuse\n\nData and metadata are in the public domain.\n\nAcknowledgments\n\nThis data set description was inspired by Middle, S. A documentation checklist for (Linked) humanities data. Int J Digit Humanities 5, 353–371 (2023). https://doi.org/10.1007/s42803-023-00072-z",
  "funding": [
    {
      "funder": {
        "@type": "Organization",
        "@id": "018mejw64",
        "name": "Deutsche Forschungsgemeinschaft"
      },
      "name": "537184692",
      "url": {
        "identifier": "https://gepris.dfg.de/gepris/projekt/537184692",
        "scheme": "url"
      }
    }
  ],
  "url": "https://datastore.uni-muenster.de/records/wjkxc-ck174",
  "distribution": [
    {
      "@type": "DataDownload",
      "contentUrl": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files/prodatphil_releases-main-0.2.zip/content",
      "encodingFormat": "application/zip"
    }
  ]
}
```

DataCite

```shell
$ curl -X GET https://datastore.uni-muenster.de/api/records/wjkxc-ck174 -H "Accept: application/vnd.datacite.datacite+xml"
<?xml version='1.0' encoding='utf-8'?>
<resource xmlns="http://datacite.org/schema/kernel-4" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4.5/metadata.xsd">
  <alternateIdentifiers>
    <alternateIdentifier alternateIdentifierType="URL">https://datastore.uni-muenster.de/records/wjkxc-ck174</alternateIdentifier>
    <alternateIdentifier alternateIdentifierType="oai">oai:datastore-rdm.com:wjkxc-ck174</alternateIdentifier>
  </alternateIdentifiers>
  <creators>
    <creator>
      <creatorName nameType="Personal">Heßbrüggen-Walter, Stefan</creatorName>
      <givenName>Stefan</givenName>
      <familyName>Heßbrüggen-Walter</familyName>
      <affiliation>Universität Münster, Zentrum für Wissenschaftstheorie (ZfW)</affiliation>
      <affiliation affiliationIdentifier="https://ror.org/00pd74e08" affiliationIdentifierScheme="ROR">Universität Münster</affiliation>
    </creator>
    <creator>
      <creatorName nameType="Personal">Frank, Ingo</creatorName>
      <givenName>Ingo</givenName>
      <familyName>Frank</familyName>
      <affiliation>Universität Münster, Universitäts- und Landesbibliothek, ULB Stabsreferat R1 Wissenschaft &amp; Innovation, ULB R1 Referat 1 Forschung und Entwicklung, ULB R1.2 Teilreferat Digital Humanities</affiliation>
      <affiliation affiliationIdentifier="https://ror.org/00pd74e08" affiliationIdentifierScheme="ROR">Universität Münster</affiliation>
    </creator>
  </creators>
  <titles>
    <title>PRODATPHIL -- Science and Logic v. 0.2</title>
    <title titleType="Subtitle">Datensammlung zu Logik und Wissenschaftsphilosophie des 19. Jahrhunderts</title>
  </titles>
  <publisher>University of Münster</publisher>
  <publicationYear>2025</publicationYear>
  <subjects>
    <subject>digital humanities</subject>
    <subject>digital philosophy</subject>
    <subject>data collection</subject>
    <subject>logic</subject>
    <subject>philosophy of science</subject>
  </subjects>
  <contributors>
    <contributor contributorType="Researcher">
      <contributorName nameType="Personal">Heßbrüggen-Walter, Stefan</contributorName>
      <givenName>Stefan</givenName>
      <familyName>Heßbrüggen-Walter</familyName>
      <affiliation>Universität Münster, Zentrum für Wissenschaftstheorie (ZfW)</affiliation>
      <affiliation affiliationIdentifier="https://ror.org/00pd74e08" affiliationIdentifierScheme="ROR">Universität Münster</affiliation>
    </contributor>
    <contributor contributorType="Researcher">
      <contributorName nameType="Personal">Frank, Ingo</contributorName>
      <givenName>Ingo</givenName>
      <familyName>Frank</familyName>
      <affiliation>Universität Münster, Universitäts- und Landesbibliothek, ULB Stabsreferat R1 Wissenschaft &amp; Innovation, ULB R1 Referat 1 Forschung und Entwicklung, ULB R1.2 Teilreferat Digital Humanities</affiliation>
      <affiliation affiliationIdentifier="https://ror.org/00pd74e08" affiliationIdentifierScheme="ROR">Universität Münster</affiliation>
    </contributor>
  </contributors>
  <dates>
    <date dateType="Issued">2025-09-26</date>
    <date dateType="Created">2025</date>
    <date dateType="Updated">2026-02-09</date>
  </dates>
  <language>deu</language>
  <resourceType resourceTypeGeneral="Dataset"></resourceType>
  <identifier identifierType="DOI">10.17879/91968401496</identifier>
  <relatedIdentifiers>
    <relatedIdentifier relationType="Continues" resourceTypeGeneral="Dataset" relatedIdentifierType="DOI">10.17879/35948496290</relatedIdentifier>
    <relatedIdentifier relationType="IsVersionOf" relatedIdentifierType="DOI">10.17879/vstfr-h2k88</relatedIdentifier>
  </relatedIdentifiers>
  <formats>
    <format>ZIP</format>
  </formats>
  <rightsList>
    <rights rightsURI="https://spdx.org/licenses/CC0-1.0" rightsIdentifierScheme="spdx" rightsIdentifier="cc0-1.0">CC0 1.0</rights>
  </rightsList>
  <descriptions>
    <description descriptionType="Abstract">This data set is an output of the DFG-funded project "Prodatphil -- Science and Logic" (project number 537184692), a cooperation of the Center of Philosophy of Science and the Service Center for Digital Humanities of the university of Münster (principal investigator Stefan Heßbrüggen-Walter). The project started in July 2024 and is currently funded until July 2027.
 
Authors in alphabetic order: Ingo Frank (metadata), Stefan Heßbrüggen-Walter (conception, construction of the corpus)
 
This is an alpha version with no guarantees and subject to change.
 
Scope
The data set aims at researchers interested in the application of DH methods to philosophical texts. It is limited to texts in the public domain from the 19th century in English. Future versions will include German and French source texts as well.
 
Please note that these are historical sources which may contain racist tropes or other content that discriminates against groups of people. Data are made available for research purposes and are not to be understood as propaganda or an endorsement of discriminatory practices.
 
Content
 
 
This corpus contains in sum 207 texts, 72 books and 135 articles. 16 texts were published anonymously. The remaining texts were written by 119 known authors and published between 1830 and 1956. Articles were published in 13 journals.
 
The English subcorpus contains in sum 164 texts, 42 books and 122 articles. 15 texts were published anonymously. The remaining texts were written by 97 known authors and published between 1839 and 1930. Articles were published in 10 journals.
 
The French subcorpus contains in sum 36 texts, 23 books and 13 articles. 1 text was published anonymously. The remaining texts were written by 20 known authors and published between 1830 and 1956. Articles were published in 3 journals.
 
The German subcorpus contains in sum 7 texts, 7 books and no articles. No texts were published anonymously. The texts were written by 6 known authors and published between 1868 and 1919. No articles were published in journals.
 
More details in the README of the data set and a notebook containing a corpus description.
 
 
Technical requirements
The data set comprises TEI files and metadata. All data can be accessed using standard software (text or XML editor, spreadsheet software, browser). No special tools are required.
 
Data provenance
We retrieved digital full texts from https://www.gutenberg.org and https://en.wikisource.org. Since the data are hosted in Germany, German copyright law applies. Therefore the date of death of the original author rather than the year of publication is the criterion for whether or not a text is in the public domain.
 
Data model
The data set contains two three CSV files containing metadata and 94 files containing TEI encoded text. The metadata format is not yet definitive and errors in the TEI encoding are possible. These known bugs will be corrected in the next update.
Some texts were published as a series of installments. The original metadata have been preserved, information linking this bibliographical information to the files in the data set can be found in metadata_series.csv.
 
Data reuse
 
Data and metadata are in the public domain.
 
Acknowledgments
 
This data set description was inspired by Middle, S. A documentation checklist for (Linked) humanities data. Int J Digit Humanities 5, 353–371 (2023). https://doi.org/10.1007/s42803-023-00072-z</description>
  </descriptions>
  <fundingReferences>
    <fundingReference>
      <funderName>Deutsche Forschungsgemeinschaft</funderName>
      <awardNumber>537184692</awardNumber>
    </fundingReference>
  </fundingReferences>
</resource>
```


Mit dem folgendem link aus der linklist weiter oben bekommt man ein file-listing:

```json
  "links": {
    "self": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174",
	...  
    "files": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files",
	...
  },
```


```shell
$ curl https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files
{
  "enabled": true,
  "links": {
    "self": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files",
    "archive": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files-archive"
  },
  "entries": [
    {
      "created": "2026-02-09T17:05:25.441278+00:00",
      "updated": "2026-02-09T17:05:26.345855+00:00",
      "mimetype": "application/zip",
      "version_id": "87e604b4-3d3c-49a2-a86a-3937aadfcc00",
      "file_id": "c8934e9f-f16f-4d56-b3bb-c38d814e1b5e",
      "bucket_id": "85b7ae2a-8a08-44fc-aff3-9a938b472e2d",
      "metadata": {
        "description": "Published dataset."
      },
      "access": {
        "hidden": false
      },
      "links": {
        "self": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files/prodatphil_releases-main-0.2.zip",
        "content": "https://datastore.uni-muenster.de/api/records/wjkxc-ck174/files/prodatphil_releases-main-0.2.zip/content"
      },
      "key": "prodatphil_releases-main-0.2.zip",
      "size": 147748661,
      "transfer": {
        "type": "L"
      },
      "status": "completed",
      "checksum": "md5:e2c00625c99c8fcd15593b9b5553cbda",
      "storage_class": "L"
    }
  ],
  "default_preview": null,
  "order": []
}
```

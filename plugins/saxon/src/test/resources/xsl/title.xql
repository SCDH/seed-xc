xquery version "3.1";

declare namespace tei = "http://www.tei-c.org/ns/1.0";

declare default element namespace "http://www.tei-c.org/ns/1.0";

declare variable $resources as xs:string external := '../samples/?select=*.tei.xml';

declare variable $collection as document-node()* := collection($resources);

declare function tei:titles ($file as document-node()) as element(tei:title)* {
    for $title in $file//tei:title
    return $title
};

<titles>
  { tei:titles($collection) }
</titles>
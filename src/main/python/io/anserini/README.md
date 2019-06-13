    
## Pyserini - Python Interface for Anserini

### Example usage 

```
import sys
sys.path += ['src/main/python/io']

from anserini.search import pysearch
from anserini.collection import pycollection
from anserini.index import pygenerator
...
```

### To use SimpleSearcher over an index

```
searcher = pysearch.SimpleSearcher('lucene-index.robust04.pos+docvectors+rawdocs')
hits = searcher.search('hubble space telescope')

# the docid of the 1st hit
hits[0].docid
...
```

### To iterate over collection and process documents

```
collection = pycollection.Collection('TrecCollection', 'path/to/disk45')
generator = pygenerator.Generator('JsoupGenerator')

for (i, fs) in enumerate(collection.segments):
    for (i, doc) in enumerate(fs):
        # foo(doc)
        # for example:

        parsed_doc = generator.generator.createDocument(doc.document)
        id = parsed_doc.get('id')               # FIELD_ID
        raw = parsed_doc.get('raw')             # FIELD_RAW
        contents = parsed_doc.get('contents')   # FIELD_BODY
```
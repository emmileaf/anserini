    
## Pyserini - Python Interface for Anserini

### Example usage 

```
anserini_root = '.' # can replace with path to anserini root directory

import os, sys
sys.path += [os.path.join(anserini_root, 'src/main/python')]

from pyserini.search import pysearch
from pyserini.collection import pycollection
from pyserini.index import pygenerator
...
```

### To search over an index

```
searcher = pysearch.SimpleSearcher('lucene-index.robust04.pos+docvectors+rawdocs')
hits = searcher.search('hubble space telescope')

# the docid of the 1st hit
hits[0].docid
...
```

### To iterate over a collection and process documents

```
collection = pycollection.Collection('TrecCollection', 'path/to/disk45')
generator = pygenerator.Generator('JsoupGenerator')

for (i, fs) in enumerate(collection):
    for (i, doc) in enumerate(fs):
        # foo(doc)
        # for example:

        parsed_doc = generator.generator.createDocument(doc.document)
        docid = parsed_doc.get('id')            # FIELD_ID
        raw = parsed_doc.get('raw')             # FIELD_RAW
        contents = parsed_doc.get('contents')   # FIELD_BODY
```
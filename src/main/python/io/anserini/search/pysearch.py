from anserini.pyclass import JSearcher, JString

import logging
logger = logging.getLogger(__name__)

class SimpleSearcher:
    
    def __init__(self, index_dir):
        self.index = index_dir
        self.searcher = JSearcher(JString(index_dir))
    
    def search(self, query):
        return self.searcher.search(JString(query))
    
            

        
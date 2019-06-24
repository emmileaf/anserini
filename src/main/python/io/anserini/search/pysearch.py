from anserini.pyclass import JSearcher, JString

import logging
logger = logging.getLogger(__name__)

class SimpleSearcher:
    
    def __init__(self, index_dir):
        self.index = index_dir
        self.searcher = JSearcher(JString(index_dir))
    
    def search(self, *args):
        if (len(args) == 1 and 
            isinstance(args[0], str)):
            return self.searcher.search(JString(args[0]))
        elif (len(args) == 2 and isinstance(args[0], str)):
            return self.searcher.search(JString(args[0]), args[1])
        elif (len(args) == 3 and isinstance(args[0], str)):
            return self.searcher.search(JString(args[0]), args[1], args[2])
        else:
            raise ValueError("Unsupported arguments for search.")
    
            

        
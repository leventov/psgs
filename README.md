Persistent Structured Graph Storage
===================================

Features:

 - Data is modelled by Java classes (see [VK test](src/ru/leventov/psgs/test/vk/)
 
 - Directed, undirected, unique, reversible edges (ex. Parent -- Child)
 
 - Keeping integrity on node/edge removals/additions
 
 - Pretty fast work with graphs, stored partially on disk
 
 But:
 
  - Single-threaded access only
  
  - Copy-on-write modifications only
  
  - Limits: 4 billions of nodes (32-bit identifiers), 256 TB max size of the database (40-bit offsets)
  
  - The single node index (lack of indexes per node class)


Implementation
--------------

B-tree index: 4096-byte nodes, 12-byte entries, 339 entries in leaf nodes, 253 entries in inner nodes.


VK test
-------

66 millions of Persons, 1,4 billions of Friendships, 7,5 GB on disk.


License
-------

The MIT License (MIT)

Copyright (c) 2013 Roma Leventov <http://www.leventov.ru>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

# waefers-bum
Unmaintained. Automatically exported from code.google.com/p/waefers-bum  Wide Area Efficient File system that is Entirely Redundant and Secure-Built Using Modules


The WAEFER file system is a low bandwidth, peer-to-peer distributed file system that using caching on each end of the connection to reduce the amount of redundant data that is transferred as well as stored. It uses a distributed hash table for storing file location and metadata. A master server may be optionally used for authentication and easy backups of data->storage location mappings. The modular system allows for the implementation of many different options including NAT traversal using the STUNT library developed at Cornell University.

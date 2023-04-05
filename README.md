# netbeansPython

[![Maven Build](https://github.com/albilu/netbeansPython/actions/workflows/maven-publish.yml/badge.svg?branch=master)](https://github.com/albilu/netbeansPython/actions/workflows/maven-publish.yml)

## User guide

Follow the [Wiki Session](https://github.com/albilu/netbeansPython/wiki) (WIP) for the full `user guide`

## Goal of netbeansPython?

This `plugin` enables [Python Programming Language](https://www.python.org/about/) support for [Apache Netbeans](https://netbeans.apache.org/).
It leverage the `Spyder IDE` community supported [Language Server Protocol](https://github.com/python-lsp/python-lsp-server)

This project is driven by the need to bring to Netbeans similar support for Python as what other main IDEs/Editors (namely `Pycharm/Spyder IDE/VScode`) offers.

### Requirements
- Python 3.7+
- Netbeans 17
- JDK 11+

## Main Features
### Project Management
- Simple Python projects type creation (with `venv/virtualenv`)
- Import projects from Sources
- Simple `Poetry` porjects creattion (common poetry commands supported)
- Run/Build projects
    ![074f01928a19095324fb65db6d19b754.png](_resources/074f01928a19095324fb65db6d19b754.png)

### LSP Server Features
- [See list of features supported](https://github.com/python-lsp/python-lsp-server#lsp-server-features)
    ![d242d82a04c729be7c6e6b0b84759c2f.png](_resources/d242d82a04c729be7c6e6b0b84759c2f.png)
- LSP Configurations available in Netbeans via: \
    `Tools -> Python Platforms -> Lsp Server` ([configuration explaination](https://github.com/python-lsp/python-lsp-server#configuration))
    ![f4ecdf1c9e7648113e8dd265b277366a.png](_resources/f4ecdf1c9e7648113e8dd265b277366a.png)

### Python Interpreters Management
- Multiple Python Interpreters Management including `venv` and `virtualenv`
    ![cad96c97900764698d62caf7d6bf49b4.png](_resources/cad96c97900764698d62caf7d6bf49b4.png)

### Packages Management
- Manage Python Packages (Install/Delete) via `Pypi` or personal Repositories \
    `Windows -> Python Package Manager`
    ![74a5dd8877b9608931b49604ab59440d.png](_resources/74a5dd8877b9608931b49604ab59440d.png)

### Python REPL
- `IPython` and `PTPython` integrated as Interactive Shells (with autocompletion) \
    `Windows -> Interactive Python Interpreters -> IPython`
    ![b18df5a8d44631819affe75ecd6fab61.png](_resources/b18df5a8d44631819affe75ecd6fab61.png)

### Unittest/Test Coverage
- Unittest/code coverage suportted
- Create/Generate unit tests
- Go to test/tested class
    ![ebaed2b72b503dec7733d6698f3a0a72.png](_resources/ebaed2b72b503dec7733d6698f3a0a72.png)

## Next Features
Here are list of features to come ordered by priority
- Debugger => TD: 30APR
- Support Pytest framework
- Profiler (inspire from the Spyder IDE one)
- Support Web development frameworks (Django/Flask)
- Jupyter Notebook/Data Science/Conda support (premisces with IPython integrated already)

## Contributing
(WIP)
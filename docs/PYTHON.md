I have wondered whether we could take the same approach as JarNode to
packaging Python.

I sat down with someone at work and went through the details.

## A plan for doing it

Instead of execing python we have to generate a script and exec that
in a shell - this is so venvs work:

```
source .venv/bin/activate
python t.py
```

and then the flow looks something like this:

```
mkdir demojarplace
cp demoapp/demo.jar demojarplace
cd demojarplace
jar xvf demo.jar 
cp ~/script . 
bash script
```

So a packaged artifact would need to include the full virtualenv of
the python program.

This means we're additionally dependent on a scripting solution, on
Linux, Bash and on Windows, batch scripts or PowerShell.

That doesn't feel like such a trial though?

## Caveats

Virtualenvs can be independent of the machine (in terms of having no
symlinks) but not the architecture.

But that feels pretty much like node. We'd expect to build on unix to
deliver a unix artifact.

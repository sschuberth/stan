# Stan - The *St*atement *An*alyzer

## What's this?

Stan is both a library and command line tool to convert and analyze bank account statements. It works completely offline by parsing the statement files you specify and does not require any online connection or [HBCI interface](http://www.hbci-zka.de/spec/spezifikation.htm).

## What statement files are supported?

- [Postbank PDF version 2014](https://www.postbank.de/privatkunden/docs/Kontoauszug_A4_Privatkunden.pdf)

  The Postbank has changed the format for PDF account statements multiple times. The only supported format is the one introduced in July 2014.

## What file formats can be exported to?

- [OFX version 1](http://www.ofx.net/downloads.html)

  The OXF files can then be imported in finance applications like [jGnash](https://ccavanaugh.github.io/jgnash/) or [GnuCash](https://www.gnucash.org/).

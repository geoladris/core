CSS files on a geoladris application come from a variety of locations. All `.css`
files in a specific location are loaded in an arbitrary order, but all `.css` files
in a location will always be loaded before the `.css` file from the next location.

Here is the ordered list of locations, with a description of what their contents are:

* `src/main/webapp/styles`: This location contain `.css` files and images from external
  libraries (such as OpenLayers). No custom styling should ever be added here.

* `src/main/webapp/modules`: This location contain `.css` files that are specific to the 
  modules contained in the same location.

* `src/main/webapp/themes`: This is where the final styling happens. All CSS rules for
  styling the whole application belong here.

* `<conf_dir>/modules`: Unlike the previous locations, this can be modified once the
  viewer has been deployed (see [configuration directory](conf_dir.md)). Any `.css` file placed here will be
  automatically included in the viewer. Note that all of these files will be directly included
  without any optimization, so if you want to shrink or merge you CSS files, you should do it before placing them here.


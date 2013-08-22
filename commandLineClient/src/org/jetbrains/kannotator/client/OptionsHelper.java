package org.jetbrains.kannotator.client;

import org.jetbrains.kannotator.annotations.io.AnnotationsFormat;
import plume.Option;

public class OptionsHelper {

    @Option(value="-f output format: jaif or xml", aliases={"format"})
    public AnnotationsFormat format = AnnotationsFormat.XML;

    @Option(value="-n produce nullability annotations", aliases={"nullability"})
    public boolean nullability = false;

    @Option(value="-m produce mutability annotations", aliases={"mutability"})
    public boolean mutability = false;

    @Option(value="-v be verbose and show progress indicator")
    public boolean verbose = false;

    @Option(value="-c do not create specific subdirectories for each library")
    public boolean one_directory_tree = false;

    @Option(value="-o output path")
    public String output_path = "annotations/";
}

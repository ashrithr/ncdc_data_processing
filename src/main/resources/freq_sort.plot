set terminal png
set output "~/Desktop/freqdist.png"

set title "Frequency Distribution of Temperature from most freq accessed to least freq accessed";
set ylabel "Number of Hits";
set xlabel "Temp (Sorted by hits)";
set key left top

plot "~/Desktop/freq_out.data" using 1:2 title "2 Node" with linespoints
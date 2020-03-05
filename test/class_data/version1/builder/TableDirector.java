package builder;

import builder.Builder;

import java.io.*;

public class TableDirector {
        private Builder m_builder;

        public TableDirector(Builder b) {
            m_builder = b;
        }

        public void construct(String file_name) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file_name));
                String line;
                String[] cells;

                if ((line = br.readLine()) != null) {
                    cells = line.split("\\t");
                    int width = Integer.parseInt(cells[0]);
                    int height = Integer.parseInt(cells[1]);
                    m_builder.set_width_and_height(width, height);
                }

                while ((line = br.readLine()) != null) {
                    cells = line.split("\\t");
                    for (int col = 0; col < cells.length; ++col) {
                        m_builder.build_cell(cells[col]);
                    }
                    m_builder.start_row();
                }

                br.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
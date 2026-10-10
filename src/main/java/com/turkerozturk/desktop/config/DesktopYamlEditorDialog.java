package com.turkerozturk.desktop.config;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/** Swing settings editor inspired by DailyTopicTracker's local properties editor. */
@SuppressWarnings("serial")
public final class DesktopYamlEditorDialog extends JDialog {
    private final boolean tr=Locale.getDefault().getLanguage().equals("tr");
    private final DesktopYamlFiles files=new DesktopYamlFiles();
    private final JComboBox<DesktopYamlFiles.Choice> choices=new JComboBox<>();
    private final JCheckBox reveal=new JCheckBox();
    private final JTextField filter=new JTextField(22);
    private final JLabel status=new JLabel(" ");
    private final JButton save=new JButton();
    private final JTabbedPane tabs=new JTabbedPane();
    private final JTextArea raw=new JTextArea();
    private final List<DesktopYamlFiles.Field> rows=new ArrayList<>();
    private final DefaultTableModel model=new DefaultTableModel(new Object[]{"Setting","Type","Value"},0){
        @Override public boolean isCellEditable(int row,int column){return column==1||column==2;}
    };
    private final JTable table=new JTable(model){
        @Override public TableCellEditor getCellEditor(int row,int column){
            int index=convertRowIndexToModel(row);
            if(column==1)return new DefaultCellEditor(new JComboBox<>(new String[]{"string","boolean","number","null"}));
            if(column==2&&"boolean".equals(model.getValueAt(index,1)))return new DefaultCellEditor(new JComboBox<>(new String[]{"true","false"}));
            if(column==2&&rows.get(index).secret()&&!reveal.isSelected())return new DefaultCellEditor(new JPasswordField());
            return super.getCellEditor(row,column);
        }
    };
    private final TableRowSorter<DefaultTableModel> sorter=new TableRowSorter<>(model);
    private DesktopYamlFiles.Snapshot snapshot;
    private String tableBase="";
    private int activeTab;
    private boolean loading;
    private boolean busy;
    private DesktopYamlFiles.Choice current;
    private String text(String turkish,String english){return tr?turkish:english;}
    public DesktopYamlEditorDialog(Frame owner){
        super(owner,"SweetCherry — YAML",true);setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        model.setColumnIdentifiers(new Object[]{text("Ayar","Setting"),text("Tür","Type"),text("Değer","Value")});
        setLayout(new BorderLayout(8,8));
        JPanel heading=new JPanel(new BorderLayout(8,8));heading.setBorder(BorderFactory.createEmptyBorder(12,12,4,12));
        heading.add(new JLabel(text("Ayar dosyası","Settings file")),BorderLayout.WEST);heading.add(choices,BorderLayout.CENTER);
        JButton reload=new JButton(text("Yeniden Oku","Reload"));heading.add(reload,BorderLayout.EAST);
        JLabel help=new JLabel(text("<html>Yerel dosyalar ve JAR varsayılanları. Kaydetmek ayarları hemen uygulamaz.<br>Güvenlik, HTTPS ve port ayarları da düzenlenebilir. Yanlış değer başlangıcı engelleyebilir.<br>Profil dosyasını düzenlemek profili etkinleştirmez; komut satırı/ortam değerleri öncelikli olabilir.</html>","<html>Local files and JAR defaults. Saving does not apply settings immediately.<br>Security, HTTPS and ports are editable too. Invalid values may prevent startup.<br>Editing a profile does not activate it; command-line/environment values may override these settings.</html>"));
        JPanel top=new JPanel(new BorderLayout(8,8));top.add(heading,BorderLayout.NORTH);top.add(help,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        JPanel tablePanel=new JPanel(new BorderLayout(5,5));JPanel search=new JPanel(new FlowLayout(FlowLayout.LEFT));
        search.add(new JLabel(text("Filtre","Filter")));search.add(filter);reveal.setText(text("Gizli değerleri göster","Show secrets"));search.add(reveal);tablePanel.add(search,BorderLayout.NORTH);
        table.setTransferHandler(null);table.setRowSorter(sorter);table.setRowHeight(26);table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(340);table.getColumnModel().getColumn(1).setPreferredWidth(80);table.getColumnModel().getColumn(2).setPreferredWidth(380);
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int column){
                boolean secret=rows.get(t.convertRowIndexToModel(row)).secret();
                return super.getTableCellRendererComponent(t,secret&&!reveal.isSelected()?"••••••••":value,selected,focus,row,column);
            }
        });
        tablePanel.add(new JScrollPane(table),BorderLayout.CENTER);tabs.addTab(text("Ayar Tablosu","Settings Table"),tablePanel);
        raw.setFont(new Font(Font.MONOSPACED,Font.PLAIN,13));raw.setTabSize(2);tabs.addTab(text("YAML Metni","YAML Text"),new JScrollPane(raw));add(tabs,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new BorderLayout());bottom.setBorder(BorderFactory.createEmptyBorder(6,12,12,12));bottom.add(status,BorderLayout.NORTH);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));save.setText(text("Kaydet","Save"));save.setEnabled(false);
        JButton restart=new JButton(text("SweetCherry’yi Yeniden Başlat","Restart SweetCherry"));JButton close=new JButton(text("Kapat","Close"));
        actions.add(save);actions.add(restart);actions.add(close);bottom.add(actions,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
        choices.addActionListener(e->{if(loading)return;var chosen=(DesktopYamlFiles.Choice)choices.getSelectedItem();if(Objects.equals(chosen,current))return;
            if(!discard()){loading=true;choices.setSelectedItem(current);loading=false;return;}load(chosen);});
        reload.addActionListener(e->{if(discard())load(current);});
        save.addActionListener(e->saveFile());close.addActionListener(e->closeDialog());
        restart.addActionListener(e->{if(busy)return;if(dirty()){JOptionPane.showMessageDialog(this,text("Önce değişiklikleri kaydedin veya yeniden okuyarak vazgeçin.","Save your changes or reload to discard them first."));return;}
            if(JOptionPane.showConfirmDialog(this,text("SweetCherry yeniden başlatılsın mı? Tüm oturumlar kapanacak.","Restart SweetCherry? All sessions will close."),"SweetCherry",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){
                if(com.turkerozturk.SweetCherry.requestRestart())dispose();else JOptionPane.showMessageDialog(this,text("Yeniden başlatma kullanılamıyor. Pencereyi kapatıp uygulamayı elle yeniden açın.","Restart is unavailable. Close this window and restart the application manually."));}
        });
        reveal.addActionListener(e->table.repaint());
        filter.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){change();}public void removeUpdate(DocumentEvent e){change();}public void changedUpdate(DocumentEvent e){change();}private void change(){String query=filter.getText();sorter.setRowFilter(query.isBlank()?null:RowFilter.regexFilter("(?i)"+Pattern.quote(query),0));}});
        tabs.addChangeListener(e->changeTab());addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){closeDialog();}});
        setSize(960,620);setMinimumSize(new Dimension(680,450));setLocationRelativeTo(owner);
        try{loading=true;for(var choice:files.choices())choices.addItem(choice);loading=false;
            if(choices.getItemCount()>0)load(choices.getItemAt(0));else status.setText(text("YAML dosyası veya JAR varsayılanı bulunamadı.","No YAML files or bundled defaults found."));
        }catch(Exception error){loading=false;showError();}
    }
    private void load(DesktopYamlFiles.Choice choice){
        if(choice==null||busy)return;busy=true;table.setEnabled(false);raw.setEnabled(false);current=choice;save.setEnabled(false);choices.setEnabled(false);status.setText(text("Okunuyor…","Loading…"));
        new SwingWorker<DesktopYamlFiles.Snapshot,Void>(){
            @Override protected DesktopYamlFiles.Snapshot doInBackground()throws Exception{return files.read(choice);}
            @Override protected void done(){busy=false;table.setEnabled(true);raw.setEnabled(true);choices.setEnabled(true);try{snapshot=get();loading=true;raw.setText(snapshot.yaml());tableBase=snapshot.yaml();
                try{fill(tableBase);tabs.setSelectedIndex(0);activeTab=0;}catch(RuntimeException error){rows.clear();model.setRowCount(0);
                    if(JOptionPane.showConfirmDialog(DesktopYamlEditorDialog.this,text("Tablo okunamadı. Düzeltmek için şifreleri de içeren YAML metni açılsın mı?","The table cannot be read. Open YAML text, including passwords, to fix it?"),"SweetCherry",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){tabs.setSelectedIndex(1);activeTab=1;}
                    else {snapshot=null;raw.setText("");loading=false;status.setText(text("YAML açılamadı; Yeniden Oku ile tekrar deneyin.","YAML not opened; Reload to try again."));return;}
                }
                loading=false;save.setEnabled(true);status.setText(snapshot.external()?text("Harici dosya: ","External file: ")+choice.relative():text("JAR varsayılanı; yalnız değiştirilip kaydedilirse harici dosya oluşturulur.","Bundled default; an external file is created only when changed and saved."));
            }catch(Exception error){loading=false;snapshot=null;showError();}}
        }.execute();
    }
    private void fill(String yaml){rows.clear();rows.addAll(DesktopYamlFiles.fields(yaml));model.setRowCount(0);for(var field:rows)model.addRow(new Object[]{field.key(),field.type(),field.value()});}
    private void finishCell(){if(table.isEditing()&&!table.getCellEditor().stopCellEditing())throw new IllegalArgumentException("Cannot finish edit");}
    private String working(){
        finishCell();if(activeTab==1)return raw.getText();Map<String,DesktopYamlFiles.Field> changes=new HashMap<>();
        for(int i=0;i<rows.size();i++){var original=rows.get(i);String type=String.valueOf(model.getValueAt(i,1)),value=String.valueOf(model.getValueAt(i,2));
            if(type.equals("null")&&!value.isBlank()&&!value.equals("null")&&!value.equals("~")){type="string";model.setValueAt(type,i,1);}
            if(!type.equals(original.type())||!value.equals(original.value()))changes.put(original.id(),new DesktopYamlFiles.Field(original.id(),original.key(),type,value,original.secret()));}
        return DesktopYamlFiles.update(tableBase,changes);
    }
    private void changeTab(){
        if(loading||busy||snapshot==null)return;int selected=tabs.getSelectedIndex();if(selected==activeTab)return;
        try{if(selected==1){if(JOptionPane.showConfirmDialog(this,text("YAML metni şifreleri açıkça gösterir. Devam edilsin mi?","YAML text shows passwords openly. Continue?"),"SweetCherry",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)throw new IllegalStateException();raw.setText(working());}
            else {String yaml=raw.getText();fill(yaml);tableBase=yaml;}
            activeTab=selected;
        }catch(Exception error){loading=true;tabs.setSelectedIndex(activeTab);loading=false;if(!(error instanceof IllegalStateException))showError();}
    }
    private boolean dirty(){if(snapshot==null)return false;try{return !working().equals(snapshot.yaml());}catch(Exception error){return true;}}
    private boolean discard(){return !busy&&(!dirty()||JOptionPane.showConfirmDialog(this,text("Kaydedilmemiş değişikliklerden vazgeçilsin mi?","Discard unsaved changes?"),"SweetCherry",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION);}
    private void closeDialog(){if(discard())dispose();}
    private void saveFile(){
        if(snapshot==null||busy)return;final String yaml;try{yaml=working();DesktopYamlFiles.validate(yaml);}catch(Exception error){showError();return;}
        if(yaml.equals(snapshot.yaml())){status.setText(text("Değişiklik yok; dosya oluşturulmadı/değiştirilmedi.","No changes; no file created or changed."));return;}
        if(JOptionPane.showConfirmDialog(this,text("Bu YAML dosyası kaydedilsin mi? Güvenlik ayarları dahil tüm değişiklikler sonraki başlangıçta kullanılabilir.","Save this YAML file? All changes, including security settings, may apply on next startup."),"SweetCherry",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        busy=true;table.setEnabled(false);raw.setEnabled(false);save.setEnabled(false);choices.setEnabled(false);tabs.setEnabled(false);
        var saving=snapshot;
        new SwingWorker<Void,Void>(){
            @Override protected Void doInBackground()throws Exception{files.save(saving,yaml);return null;}
            @Override protected void done(){busy=false;table.setEnabled(true);raw.setEnabled(true);tabs.setEnabled(true);choices.setEnabled(true);save.setEnabled(true);
                try{get();JOptionPane.showMessageDialog(DesktopYamlEditorDialog.this,text("Kaydedildi. Ayarları uygulamak için yeniden başlatın.","Saved. Restart to apply the settings."));load(current);}catch(Exception error){showError();}}
        }.execute();
    }
    /** Never show YAML parser exception text: it may quote credentials. */
    private void showError(){JOptionPane.showMessageDialog(this,text("İşlem tamamlanamadı. YAML biçimini, değer türünü, dosya izinlerini ve başka yerde değiştirilip değiştirilmediğini kontrol edin. Yeniden Oku ile güncel dosyayı açın. Ayrıntılı hata metni gizli değerler içerebileceği için gösterilmez.","Could not complete the operation. Check YAML syntax, value types, file permissions and external changes. Reload the current file. Detailed errors are hidden because they may contain secrets."),"SweetCherry",JOptionPane.WARNING_MESSAGE);}
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class TodoApp extends JFrame {
    private JTextField taskField;
    private JList<String> taskList;
    private DefaultListModel<String> listModel;
    private Connection conn;
    private ArrayList<Integer> taskIds;

    public TodoApp() {
        setTitle("Todo List App");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Database setup
        initDatabase();

        // GUI setup
        setLayout(new BorderLayout(10, 10));

        // Task input
        taskField = new JTextField(40);
        JButton addButton = new JButton("Add Task");
        addButton.addActionListener(e -> addTask());

        JPanel inputPanel = new JPanel();
        inputPanel.add(taskField);
        inputPanel.add(addButton);
        add(inputPanel, BorderLayout.NORTH);

        // Task list
        listModel = new DefaultListModel<>();
        taskList = new JList<>(listModel);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = taskList.getSelectedIndex();
                if (index >= 0) {
                    String task = listModel.get(index).split("] ")[1];
                    taskField.setText(task);
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(taskList);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton updateButton = new JButton("Update Task");
        updateButton.addActionListener(e -> updateTask());
        JButton deleteButton = new JButton("Delete Task");
        deleteButton.addActionListener(e -> deleteTask());
        JButton completeButton = new JButton("Mark Complete");
        completeButton.addActionListener(e -> markComplete());

        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(completeButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Load tasks
        loadTasks();

        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (conn != null)
                        conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void initDatabase() {
        try {
            // Replace with your MySQL credentials
            String url = "jdbc:mysql://localhost:3306/todo?useSSL=false";
            String user = "root"; // Your MySQL username
            String password = ""; // Your MySQL password
            conn = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void addTask() {
        String task = taskField.getText().trim();
        if (task.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a task!");
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO tasks (task, completed, created_at) VALUES (?, ?, ?)");
            ps.setString(1, task);
            ps.setBoolean(2, false);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
            ps.close();
            taskField.setText("");
            loadTasks();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding task: " + e.getMessage());
        }
    }

    private void loadTasks() {
        listModel.clear();
        taskIds = new ArrayList<>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, task, completed FROM tasks ORDER BY created_at DESC");
            while (rs.next()) {
                int id = rs.getInt("id");
                String task = rs.getString("task");
                boolean completed = rs.getBoolean("completed");
                listModel.addElement("[" + (completed ? "✓" : " ") + "] " + task);
                taskIds.add(id);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading tasks: " + e.getMessage());
        }
    }

    private void updateTask() {
        int index = taskList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task to update!");
            return;
        }

        String newTask = taskField.getText().trim();
        if (newTask.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a task!");
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tasks SET task = ? WHERE id = ?");
            ps.setString(1, newTask);
            ps.setInt(2, taskIds.get(index));
            ps.executeUpdate();
            ps.close();
            taskField.setText("");
            loadTasks();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating task: " + e.getMessage());
        }
    }

    private void deleteTask() {
        int index = taskList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task to delete!");
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM tasks WHERE id = ?");
            ps.setInt(1, taskIds.get(index));
            ps.executeUpdate();
            ps.close();
            taskField.setText("");
            loadTasks();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting task: " + e.getMessage());
        }
    }

    private void markComplete() {
        int index = taskList.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Please select a task to mark as complete!");
            return;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tasks SET completed = ? WHERE id = ?");
            ps.setBoolean(1, true);
            ps.setInt(2, taskIds.get(index));
            ps.executeUpdate();
            ps.close();
            loadTasks();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error marking task: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new TodoApp().setVisible(true);
        });
    }
}
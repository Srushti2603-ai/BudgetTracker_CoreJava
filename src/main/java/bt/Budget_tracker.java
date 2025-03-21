package bt;

import java.sql.*;
import java.util.Scanner;

public class Budget_tracker {
    private static final String URL = "jdbc:postgresql://localhost:5432/budget_db";
    private static final String USER = "postgres";
    private static final String PASSWD = "123";

    // Get Database Connection
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWD);
    }

    // Add Transaction (with budget check)
    private static void addTransaction(String type, String category, float amount, String description) throws SQLException {
        if (type.equalsIgnoreCase("Expense")) {
            float budgetLimit = getBudget(category);
            float totalExpenses = getTotalExpenses(category);

            if (budgetLimit > 0 && (totalExpenses + amount) > budgetLimit) {
                System.out.printf("⚠ Warning: This expense exceeds the budget for '%s'! Budget Limit: %.2f, Current Expenses: %.2f%n",
                        category, budgetLimit, totalExpenses);
                System.out.print("Do you want to proceed anyway? (yes/no): ");
                Scanner scanner = new Scanner(System.in);
                String response = scanner.nextLine().trim().toLowerCase();
                if (!response.equals("yes")) {
                    System.out.println("Transaction canceled.");
                    return;
                }
            }
        }

        String query = "INSERT INTO transactions (type, category, amount, description) VALUES (?, ?, ?, ?)";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setString(1, type);
            stmt.setString(2, category);
            stmt.setFloat(3, amount);
            stmt.setString(4, description);
            stmt.executeUpdate();
            System.out.println("Transaction added successfully!");
        }
    }

    // View All Transactions
    private static void viewTransactions() throws SQLException {
        String query = "SELECT * FROM transactions ORDER BY transaction_date DESC";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            System.out.println("\nID | Type | Category | Amount | Description | Date");
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %.2f | %s | %s%n",
                        rs.getInt("id"), rs.getString("type"), rs.getString("category"),
                        rs.getFloat("amount"), rs.getString("description"), rs.getTimestamp("transaction_date"));
            }
        }
    }

    // Update a Transaction
    private static void updateTransaction(int id, String type, String category, float amount, String description) throws SQLException {
        String query = "UPDATE transactions SET type = ?, category = ?, amount = ?, description = ? WHERE id = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setString(1, type);
            stmt.setString(2, category);
            stmt.setFloat(3, amount);
            stmt.setString(4, description);
            stmt.setInt(5, id);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Transaction updated successfully!");
            } else {
                System.out.println("Transaction ID not found.");
            }
        }
    }

    // Delete a Transaction
    private static void deleteTransaction(int id) throws SQLException {
        String query = "DELETE FROM transactions WHERE id = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setInt(1, id);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Transaction deleted successfully!");
            } else {
                System.out.println("Transaction ID not found.");
            }
        }
    }
    // Calculate Balance (Income - Expense)
    private static void calculateBalance() throws SQLException {
        String incomeQuery = "SELECT COALESCE(SUM(amount), 0) AS total_income FROM transactions WHERE type = 'Income'";
        String expenseQuery = "SELECT COALESCE(SUM(amount), 0) AS total_expense FROM transactions WHERE type = 'Expense'";

        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {

            ResultSet incomeRs = stmt.executeQuery(incomeQuery);
            float totalIncome = incomeRs.next() ? incomeRs.getFloat("total_income") : 0;

            ResultSet expenseRs = stmt.executeQuery(expenseQuery);
            float totalExpense = expenseRs.next() ? expenseRs.getFloat("total_expense") : 0;

            float balance = totalIncome - totalExpense;
            System.out.printf("Total Income: %.2f | Total Expense: %.2f | Balance: %.2f%n", totalIncome, totalExpense, balance);
        }
    }

    // Set or Update Budget for an Expense Category
    private static void setBudget(String category, float budgetLimit) throws SQLException {
        String query = "INSERT INTO budgets (category, budget_limit) VALUES (?, ?) ON CONFLICT (category) DO UPDATE SET budget_limit = EXCLUDED.budget_limit";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setString(1, category);
            stmt.setFloat(2, budgetLimit);
            stmt.executeUpdate();
            System.out.println("Budget set successfully for category: " + category);
        }
    }

    // Get Budget for a Category
    private static float getBudget(String category) throws SQLException {
        String query = "SELECT budget_limit FROM budgets WHERE category = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getFloat("budget_limit");
            }
        }
        return -1; // Return -1 if no budget is set
    }

    // Get Total Expenses for a Category
    private static float getTotalExpenses(String category) throws SQLException {
        String query = "SELECT COALESCE(SUM(amount), 0) AS total_expense FROM transactions WHERE category = ? AND type = 'Expense'";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getFloat("total_expense");
            }
        }
        return 0;
    }

    // Main Menu
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        while (true) {
            System.out.println("\nBudget Tracker Menu:");
            System.out.println("1. Add Transaction");
            System.out.println("2. View Transactions");
            System.out.println("3. Update Transaction");
            System.out.println("4. Delete Transaction");
            System.out.println("5. Check Balance");
            System.out.println("6. Set Budget for Category");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine();

            try {
                switch (choice) {
                    case 1:
                        System.out.print("Enter Type (Income/Expense): ");
                        String type = scanner.nextLine().trim();
                        System.out.print("Enter Category: ");
                        String category = scanner.nextLine();
                        System.out.print("Enter Amount: ");
                        float amount = scanner.nextFloat();
                        scanner.nextLine();
                        System.out.print("Enter Description: ");
                        String description = scanner.nextLine();
                        addTransaction(type, category, amount, description);
                        break;
                    case 2: viewTransactions(); break;
                    case 3:
                        System.out.print("Enter Transaction ID to update: ");
                        int updateId = scanner.nextInt();
                        scanner.nextLine();
                        System.out.print("Enter New Type (Income/Expense): ");
                        String newType = scanner.nextLine();
                        System.out.print("Enter New Category: ");
                        String newCategory = scanner.nextLine();
                        System.out.print("Enter New Amount: ");
                        float newAmount = scanner.nextFloat();
                        scanner.nextLine();
                        System.out.print("Enter New Description: ");
                        String newDescription = scanner.nextLine();
                        updateTransaction(updateId, newType, newCategory, newAmount, newDescription);
                        break;
                    case 4:
                        System.out.print("Enter Transaction ID to delete: ");
                        int deleteId = scanner.nextInt();
                        deleteTransaction(deleteId);
                        break;
                    case 5: calculateBalance(); break;
                    case 6:
                        System.out.print("Enter Category: ");
                        String budgetCategory = scanner.nextLine();
                        System.out.print("Enter Budget Limit: ");
                        float budgetLimit = scanner.nextFloat();
                        scanner.nextLine();
                        setBudget(budgetCategory, budgetLimit);
                        break;
                    case 7:
                        System.out.println("Exiting...");
                        scanner.close();
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice, please try again.");
                }
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}

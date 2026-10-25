using Kono.Api.Domain;
using Microsoft.EntityFrameworkCore;

namespace Kono.Api.Data;

public class KonoDbContext : DbContext
{
    public KonoDbContext(DbContextOptions<KonoDbContext> options) : base(options) { }

    public DbSet<Vertical> Verticals => Set<Vertical>();
    public DbSet<Store> Stores => Set<Store>();
    public DbSet<StoreTag> StoreTags => Set<StoreTag>();
    public DbSet<MenuCategory> MenuCategories => Set<MenuCategory>();
    public DbSet<Product> Products => Set<Product>();
    public DbSet<ProductOptionGroup> ProductOptionGroups => Set<ProductOptionGroup>();
    public DbSet<ProductOption> ProductOptions => Set<ProductOption>();
    public DbSet<Promotion> Promotions => Set<Promotion>();

    public DbSet<User> Users => Set<User>();
    public DbSet<RefreshToken> RefreshTokens => Set<RefreshToken>();
    public DbSet<Address> Addresses => Set<Address>();
    public DbSet<PaymentMethod> PaymentMethods => Set<PaymentMethod>();
    public DbSet<Courier> Couriers => Set<Courier>();

    public DbSet<Order> Orders => Set<Order>();
    public DbSet<OrderItem> OrderItems => Set<OrderItem>();
    public DbSet<OrderEvent> OrderEvents => Set<OrderEvent>();

    public DbSet<Conversation> Conversations => Set<Conversation>();
    public DbSet<ChatMessage> ChatMessages => Set<ChatMessage>();
    public DbSet<CallLog> CallLogs => Set<CallLog>();

    protected override void OnModelCreating(ModelBuilder b)
    {
        base.OnModelCreating(b);

        // store every enum as a readable string
        foreach (var entity in b.Model.GetEntityTypes())
            foreach (var prop in entity.ClrType.GetProperties()
                         .Where(p => p.PropertyType.IsEnum ||
                                     (Nullable.GetUnderlyingType(p.PropertyType)?.IsEnum ?? false)))
                b.Entity(entity.ClrType).Property(prop.Name).HasConversion<string>().HasMaxLength(32);

        // money precision
        foreach (var entity in b.Model.GetEntityTypes())
            foreach (var prop in entity.GetProperties()
                         .Where(p => p.ClrType == typeof(decimal) || p.ClrType == typeof(decimal?)))
                prop.SetPrecision(10);
        foreach (var entity in b.Model.GetEntityTypes())
            foreach (var prop in entity.GetProperties()
                         .Where(p => p.ClrType == typeof(decimal) || p.ClrType == typeof(decimal?)))
                prop.SetScale(2);

        b.Entity<Vertical>().HasIndex(v => v.Kind).IsUnique();
        b.Entity<User>().HasIndex(u => u.Phone).IsUnique();
        b.Entity<User>().HasIndex(u => u.Email).IsUnique().HasFilter(null);
        b.Entity<RefreshToken>().HasIndex(t => t.Token).IsUnique();
        b.Entity<Order>().HasIndex(o => o.ShortCode).IsUnique();
        b.Entity<Order>().HasIndex(o => o.Stage);
        b.Entity<Store>().HasIndex(s => s.Kind);
        b.Entity<Product>().HasIndex(p => p.StoreId);

        b.Entity<Courier>().HasOne(c => c.User).WithOne(u => u.CourierProfile)
            .HasForeignKey<Courier>(c => c.UserId).OnDelete(DeleteBehavior.Cascade);

        b.Entity<Store>().HasOne(s => s.Owner).WithMany()
            .HasForeignKey(s => s.OwnerUserId).OnDelete(DeleteBehavior.SetNull);

        b.Entity<Product>().HasOne(p => p.Store).WithMany(s => s.Products)
            .HasForeignKey(p => p.StoreId).OnDelete(DeleteBehavior.Cascade);
        b.Entity<Product>().HasOne(p => p.MenuCategory).WithMany(c => c.Products)
            .HasForeignKey(p => p.MenuCategoryId).OnDelete(DeleteBehavior.Cascade);

        b.Entity<Order>().HasOne(o => o.Customer).WithMany()
            .HasForeignKey(o => o.CustomerUserId).OnDelete(DeleteBehavior.Restrict);
        b.Entity<Order>().HasOne(o => o.Store).WithMany()
            .HasForeignKey(o => o.StoreId).OnDelete(DeleteBehavior.Restrict);
        b.Entity<Order>().HasOne(o => o.Courier).WithMany()
            .HasForeignKey(o => o.CourierId).OnDelete(DeleteBehavior.SetNull);
        b.Entity<OrderItem>().HasOne(i => i.Product).WithMany()
            .HasForeignKey(i => i.ProductId).OnDelete(DeleteBehavior.SetNull);

        b.Entity<Conversation>().HasIndex(c => new { c.OrderId, c.Topic }).IsUnique();
        b.Entity<Conversation>().HasIndex(c => c.PartyAUserId);
        b.Entity<Conversation>().HasIndex(c => c.PartyBUserId);
        b.Entity<Conversation>().HasOne(c => c.Order).WithMany()
            .HasForeignKey(c => c.OrderId).OnDelete(DeleteBehavior.Cascade);
        b.Entity<ChatMessage>().HasOne(m => m.Conversation).WithMany(c => c.Messages)
            .HasForeignKey(m => m.ConversationId).OnDelete(DeleteBehavior.Cascade);
        b.Entity<ChatMessage>().HasIndex(m => new { m.ConversationId, m.Id });
        b.Entity<ChatMessage>().Property(m => m.Text).HasMaxLength(4000);
        b.Entity<CallLog>().HasOne(c => c.Conversation).WithMany()
            .HasForeignKey(c => c.ConversationId).OnDelete(DeleteBehavior.Cascade);
    }

    public override int SaveChanges()
    {
        Touch();
        return base.SaveChanges();
    }

    public override Task<int> SaveChangesAsync(CancellationToken ct = default)
    {
        Touch();
        return base.SaveChangesAsync(ct);
    }

    private void Touch()
    {
        foreach (var e in ChangeTracker.Entries<BaseEntity>())
        {
            if (e.State == EntityState.Added) e.Entity.CreatedAt = DateTime.UtcNow;
            if (e.State is EntityState.Added or EntityState.Modified) e.Entity.UpdatedAt = DateTime.UtcNow;
        }
    }
}

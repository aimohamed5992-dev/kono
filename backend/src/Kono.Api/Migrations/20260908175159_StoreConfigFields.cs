using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Kono.Api.Migrations
{
    /// <inheritdoc />
    public partial class StoreConfigFields : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<double>(
                name: "DeliveryRadiusKm",
                table: "Stores",
                type: "double",
                nullable: false,
                defaultValue: 0.0);

            migrationBuilder.AddColumn<string>(
                name: "IbanLast4",
                table: "Stores",
                type: "longtext",
                nullable: false)
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.AddColumn<string>(
                name: "OpeningHoursAr",
                table: "Stores",
                type: "longtext",
                nullable: false)
                .Annotation("MySql:CharSet", "utf8mb4");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "DeliveryRadiusKm",
                table: "Stores");

            migrationBuilder.DropColumn(
                name: "IbanLast4",
                table: "Stores");

            migrationBuilder.DropColumn(
                name: "OpeningHoursAr",
                table: "Stores");
        }
    }
}

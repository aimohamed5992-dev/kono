using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Kono.Api.Migrations
{
    /// <inheritdoc />
    public partial class OnboardingApplication : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "OnboardingJson",
                table: "Users",
                type: "longtext",
                nullable: false)
                .Annotation("MySql:CharSet", "utf8mb4");

            migrationBuilder.AddColumn<string>(
                name: "OnboardingStatus",
                table: "Users",
                type: "longtext",
                nullable: false)
                .Annotation("MySql:CharSet", "utf8mb4");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "OnboardingJson",
                table: "Users");

            migrationBuilder.DropColumn(
                name: "OnboardingStatus",
                table: "Users");
        }
    }
}

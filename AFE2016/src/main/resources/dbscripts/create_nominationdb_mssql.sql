USE [NOMINATION]
GO

/****** Object:  Table [dbo].[nomination]    Script Date: 21-11-2016 14:12:11 ******/
DROP TABLE [dbo].[nomination]
GO

/****** Object:  Table [dbo].[nomination]    Script Date: 21-11-2016 14:12:11 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [dbo].[nomination](
	[nominationId] [int] IDENTITY(1,1) NOT NULL,
	[submitterMailNickname] [varchar](100) NULL,
	[submitterMail] [varchar](200) NULL,
	[status] [varchar](100) NULL,
	[selectedCategory] [varchar](100) NULL,
	[summary] [varchar](1000) NULL,
	[nomination] [varchar](100) NOT NULL,
	[sponsorMailNickname] [varchar](200) NULL,
	[sponsorMail] [varchar](200) NULL,
	[sponsorname] [varchar](100) NULL,
	[sponsornumber] [varchar](100) NULL,
	[filekey] [varchar](100) NULL,
	[filename] [varchar](100) NULL,
	[filetype] [varchar](100) NULL,
	[filesize] [varchar](100) NULL,
PRIMARY KEY CLUSTERED 
(
	[nominationId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO

SET ANSI_PADDING OFF
GO



ALTER TABLE [dbo].[members] DROP CONSTRAINT [FK_members_nomination]
GO

/****** Object:  Table [dbo].[members]    Script Date: 21-11-2016 14:11:55 ******/
DROP TABLE [dbo].[members]
GO

/****** Object:  Table [dbo].[members]    Script Date: 21-11-2016 14:11:55 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

CREATE TABLE [dbo].[members](
	[membersId] [int] IDENTITY(1,1) NOT NULL,
	[nominationId] [int] NOT NULL,
	[employeeMailNickname] [varchar](100) NOT NULL,
	[employeeMail] [varchar](200) NOT NULL,
	[eNo] [varchar](100) NOT NULL,
	[Name] [varchar](100) NOT NULL,
	[Unit] [varchar](100) NOT NULL,
	[contribution] [varchar](1000) NOT NULL
) ON [PRIMARY]

GO

SET ANSI_PADDING OFF
GO

ALTER TABLE [dbo].[members]  WITH CHECK ADD  CONSTRAINT [FK_members_nomination] FOREIGN KEY([nominationId])
REFERENCES [dbo].[nomination] ([nominationId])
GO

ALTER TABLE [dbo].[members] CHECK CONSTRAINT [FK_members_nomination]
GO



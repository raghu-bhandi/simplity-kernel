USE [SMTPService]
GO

/****** Object:  Table [dbo].[nomination]    Script Date: 21-11-2016 14:12:11 ******/
/*DROP TABLE [dbo].[registration]
GO*/

/****** Object:  Table [dbo].[nomination]    Script Date: 21-11-2016 14:12:11 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[registration] (
  [registrationId] [int] IDENTITY(1,1) NOT NULL,
  [submitterId]  [varchar] (100) NULL,
  [domain] [varchar] (100) NULL,
  [application]  [varchar] (100) NULL,
  [mailid] [varchar] (100) NULL,
  [apikey]  [varchar] (100) NULL,
PRIMARY KEY CLUSTERED 
(
	[registrationId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]



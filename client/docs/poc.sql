USE [xel]
GO

/****** Object:  Table [misuser].[t1]    Script Date: 4/29/2016 8:59:28 AM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [misuser].[t1](
	[id] [numeric](9, 0) NOT NULL,
	[name] [nvarchar](50) NOT NULL,
	[description] [nvarchar](150) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO

USE [xel]
GO

/****** Object:  Table [misuser].[t2]    Script Date: 4/29/2016 8:59:53 AM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [misuser].[t2](
	[id] [numeric](9, 0)  NOT NULL,
	[parentId] [numeric](9, 0) NOT NULL,
	[qty] [numeric](6, 0) NOT NULL,
	[required] [bit] NOT NULL,
	[name] [nvarchar](50) NOT NULL,
	[description] [nvarchar](150) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO

USE [xel]
GO

/****** Object:  Table [misuser].[t3]    Script Date: 4/29/2016 9:00:08 AM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [misuser].[t3](
	[id] [numeric](9, 0) NOT NULL,
	[parentId] [numeric](9, 0) NOT NULL,
	bomId [numeric](9, 0) NOT NULL,
	[qty] [numeric](6, 0) NOT NULL,
	[required] [bit] NOT NULL,
	[name] [nvarchar](50) NOT NULL,
	[description] [nvarchar](150) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO





